/*The MIT License (MIT)

Copyright (c) 2014 Brian Nakayama

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package world;

import sprite.Img;

/**
 * The basic colliding object. All collisions happen with a SimpleSolid.
 * 
 * Furthermore, the SimpleSolid automatically sorts its z-index based off of the
 * y-index on the screen.
 * 
 * @author Brian Nakayama
 * @version 1.2 Now all code is abstracted as MVC.
 * @since 1.1
 */
public abstract class SimpleSolid extends SimpleObject {

	/**
	 * Create a basic SimpleSolid.
	 */
	public SimpleSolid() {
		super(NO_COLLIDES);
	}

	/**
	 * Create a SimpleSolid optimized for moving.
	 * 
	 * @param NO_UPDATES
	 *            True if you need the object to not move.
	 */
	public SimpleSolid(boolean NO_UPDATES) {
		super(NO_UPDATES_NO_COLLIDES);
		if (!NO_UPDATES) {
			this.updates = NO_COLLIDES;
		}
	}

	/**
	 * Create a SimpleSolid with an image.
	 * 
	 * Note that this is not the good way to create a solid with an image,
	 * though it is the most convenient. Images should be loaded statically. See
	 * the same constructor for SimpleSolid for more notes.
	 * 
	 * @param sprite
	 *            The path to the image.
	 * @see SimpleObject#SimpleObject(String)
	 */
	public SimpleSolid(Img sprite) {
		super(sprite, NO_COLLIDES);
	}

	/**
	 * Create a SimpleSolid with an image, optimized for moving.
	 * 
	 * Note that this is not the good way to create a solid with an image,
	 * though it is the most convenient. Images should be loaded statically. See
	 * the same constructor for SimpleSolid for more notes.
	 * 
	 * @param sprite
	 *            The path to the image.
	 * @param NO_UPDATES
	 *            True if you need the object to move.
	 * @see SimpleObject#SimpleObject(String)
	 */
	public SimpleSolid(Img sprite, boolean NO_UPDATES) {
		super(sprite, NO_UPDATES_NO_COLLIDES);
		if (!NO_UPDATES) {
			this.updates = NO_COLLIDES;
		}
	}

	/**
	 * Undo the most recent move.
	 * 
	 * @return True if the move was successfully reversed.
	 */
	public boolean cancelMove() {
		if (move(pre_cx, pre_cy, false)) {
			pre_cx = coor_x;
			pre_cy = coor_y;
			return true;
		}
		return false;
	}

	/**
	 * The method for moving a SimpleSolid.
	 * 
	 * Similar to {@link SimpleObject#move(int, int, boolean)}, with the
	 * exception that it does not allow movements that overlap with another
	 * SimpleSolid's space. Furthermore, as of version 1.0 it does not attempt
	 * to move at all if encountering an overlap (TODO). The solids resort
	 * themselves when the y-axis has changed rows in the map.
	 * 
	 * @param x
	 *            The new x-coordinate.
	 * @param y
	 *            The new y-coordinate.
	 * @param relative
	 *            True to move relative from your position, else move
	 *            absolutely.
	 * @return True iff the object moved successfully.
	 */
	public boolean move(int x, int y, boolean relative) {
		return move(x, y, relative, 0);
	}

	/**
	 * The method for moving a solid, that will attempt to move when colliding
	 * on a corner.
	 * 
	 * Identical to {@link SimpleSolid#move(int, int, boolean)}, except if
	 * there's a collision with only one object this method will move the object
	 * by the given fuzz amount in a favorable alignment so that this object is
	 * better situated to move when move is called again. Specifically, if we
	 * collide within cellWidth / 2 or cellHeight / 2 of a corner we move the
	 * object fuzz pixels towards the tip of the corner. This method always
	 * moves the object relative from its current position, and is not intended
	 * for large leaps.
	 * 
	 * @param x
	 *            The new x-coordinate.
	 * @param y
	 *            The new y-coordinate.
	 * @param fuzz
	 *            The alternative amount to move either NS or EW if there's a
	 *            collision.
	 * @return True iff the intended move was made or the fuzz move.
	 */
	public boolean move(int x, int y, int fuzz) {
		return move(x, y, true, fuzz);
	}

	private boolean move(int x, int y, boolean relative, int fuzz) {
		if (relative) {
			x += coor_x;
			y += coor_y;
		}

		// Check if the object is trying to leave the map.
		if (x > 0) {
			if (x > m.mapWmax) {
				x = m.mapWmax;
			}
		} else {
			x = 0;
		}

		if (y > 0) {
			if (y > m.mapHmax) {
				y = m.mapHmax;
			}
		} else {
			y = 0;
		}

		// Calculate if the move is feasible.
		m.calculateCollisions(x, y, this);
		boolean isMe = (collisions[0] == this);

		if (collisions[0] == null || (isMe && collisions[1] == null)) {

			int pre_y = coor_y / m.cellHeight;
			int new_y = y / m.cellHeight;

			int relY = y / m.cellHeight - coor_y / m.cellWidth;

			pre_cx = coor_x;
			pre_cy = coor_y;
			coor_x = x;
			coor_y = y;

			m.map[pre_y][pre_cx / m.cellWidth] = null;
			m.map[new_y][coor_x / m.cellWidth] = this;

			/*
			 * Only if we've made a significant change in the y direction do we
			 * need to do the complicated sorting part.
			 */
			if (relY == 0) {
				return true;
			} else {
				// Remove from old position
				drawNext.drawPrevious = drawPrevious;
				drawPrevious.drawNext = drawNext;
				// Insert into new position
				drawPrevious = m.mapArray[new_y].drawPrevious;
				drawNext = m.mapArray[new_y];
				drawPrevious.drawNext = this;
				drawNext.drawPrevious = this;
				return true;
			}
		} else { /* Notify all objects in the collision list. */
			switch (fuzz) {
			default:
				if (collisions[2] == null) {
					System.out.println(collisions[0].getClass().getName());
					final int dx;
					final int dy;
					if (isMe) {
						dx = collisions[1].coor_x - collisions[0].coor_x;
						dy = collisions[1].coor_y - collisions[0].coor_y;
					} else {
						dx = collisions[0].coor_x - collisions[1].coor_x;
						dy = collisions[0].coor_y - collisions[1].coor_y;
					}

					if (dx > m.cellWidth / 2 && dx < m.cellWidth) {
						return move(-fuzz, 0, true, 0);
					} else if (-dx > m.cellWidth / 2 && -dx < m.cellWidth) {
						return move(fuzz, 0, true, 0);
					}

					if (dy > m.cellHeight / 2 && dy < m.cellHeight) {
						return move(0, -fuzz, true, 0);
					} else if (-dy > m.cellHeight / 2 && -dy < m.cellHeight) {
						return move(0, fuzz, true, 0);
					}
				}
				/* no break */
			case 0:
				for (SimpleSolid S : collisions) {
					if (S != null) {
						if (S != this) {
							S.collision(this);
							collision(S);
						}
					} else {
						break;
					}
				}
				break;
			}

		}

		return false;
	}

	/**
	 * Get the solid version of this object.
	 * 
	 * @return The SimpleSolid pointer for this object.
	 */
	public SimpleSolid getSolid() {
		return this;
	}

	/**
	 * Attempt to remove the Object from any map it may be a part of.
	 * 
	 * @return True if the object belongs to a map and is removed.
	 */
	public boolean removeSelf() {
		if (drawNext != null && drawPrevious != null) {
			final int x_n = coor_x / m.cellWidth;
			final int y_n = coor_y / m.cellHeight;
			m.map[y_n][x_n] = null;
			m = null;
			drawNext.drawPrevious = drawPrevious;
			drawPrevious.drawNext = drawNext;
			return true;
		}
		return false;
	}

	/**
	 * Get the solid from the specified coordinates.
	 * 
	 * @param x
	 *            The x coordinate.
	 * @param y
	 *            The y coordinate.
	 * @param relative
	 *            True for relative coordinates from the current object, false
	 *            for absolute.
	 * @return The SimpleSolid. False if the solid does not exist.
	 */
	public SimpleSolid getSolid(int x, int y, boolean relative) {
		if (relative) {
			x += coor_x;
			y += coor_y;
		}
		return m.map[y / m.cellHeight][x / m.cellWidth];
	}
}
