/*
 * This file is part of Vanilla (http://www.spout.org/).
 *
 * Vanilla is licensed under the SpoutDev License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.vanilla.protocol.event;

import org.spout.api.protocol.event.ProtocolEvent;
import org.spout.vanilla.protocol.msg.HealthMessage;

public class HealthEvent extends ProtocolEvent {
	private final short health, food;
	private final float saturation;

	public HealthEvent(short health, short food, float saturation) {
		this.health = health;
		this.food = food;
		this.saturation = saturation;
	}

	public HealthEvent(HealthMessage message) {
		this(message.getHealth(), message.getFood(), message.getFoodSaturation());
	}

	public short getHealth() {
		return health;
	}

	public short getFood() {
		return food;
	}

	public float getFoodSaturation() {
		return saturation;
	}
}
