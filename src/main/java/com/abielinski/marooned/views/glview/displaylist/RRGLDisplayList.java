/*******************************************************************************
 * This file is part of Marooned.
 *
 * Marooned is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Marooned is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Marooned.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.abielinski.marooned.views.glview.displaylist;

import com.abielinski.marooned.views.glview.program.RRGLMatrixStack;

public final class RRGLDisplayList extends RRGLRenderableGroup {

	@Override
	protected void renderInternal(final RRGLMatrixStack matrixStack, final long time) {
		super.renderInternal(matrixStack, time);
	}

	@Override
	public boolean isAdded() {
		return true;
	}
}
