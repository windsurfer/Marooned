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

package com.abielinski.marooned.test.general;

import org.junit.Test;
import com.abielinski.marooned.common.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OptionalTest {

	@Test
	public void testOptional() {

		assertEquals(Optional.empty(), Optional.empty());
		assertNotEquals(Optional.empty(), Optional.of(123));

		assertEquals(Optional.of(123), Optional.of(123));
		assertNotEquals(Optional.of(123), Optional.of(456));

		assertNotEquals(Optional.of(new Object()), Optional.of(new Object()));

		assertFalse(Optional.empty().isPresent());
		assertTrue(Optional.of(123).isPresent());

		assertEquals("Hello", Optional.of("Hello").get());
		assertThrows(Optional.OptionalHasNoValueException.class, Optional.empty()::get);

		assertEquals("Hello", Optional.of("Hello").orElse("Alternative"));
		assertEquals("Alternative", Optional.empty().orElse("Alternative"));

		assertEquals(Optional.empty(), Optional.ofNullable(null));
		assertEquals(Optional.of("Test"), Optional.ofNullable("Test"));

		//noinspection ResultOfMethodCallIgnored
		assertThrows(
				RuntimeException.class,
				() -> Optional.empty().orThrow(new RuntimeException()));

		assertEquals("Test", Optional.of("Test").orThrow(new RuntimeException()));
	}
}
