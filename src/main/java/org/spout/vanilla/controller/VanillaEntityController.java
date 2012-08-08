/*
 * This file is part of Vanilla.
 *
 * Copyright (c) 2011-2012, VanillaDev <http://www.spout.org/>
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
package org.spout.vanilla.controller;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import org.spout.api.Source;
import org.spout.api.Spout;
import org.spout.api.collision.BoundingBox;
import org.spout.api.collision.CollisionModel;
import org.spout.api.collision.CollisionStrategy;
import org.spout.api.entity.Entity;
import org.spout.api.entity.component.controller.BasicController;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.inventory.ItemStack;
import org.spout.api.math.MathHelper;
import org.spout.api.math.Quaternion;
import org.spout.api.math.Vector2;
import org.spout.api.math.Vector3;
import org.spout.api.player.Player;
import org.spout.api.protocol.event.ProtocolEvent;
import org.spout.api.tickable.LogicPriority;

import org.spout.vanilla.controller.component.basic.FireDamageComponent;
import org.spout.vanilla.controller.component.basic.HealthComponent;
import org.spout.vanilla.controller.component.physics.BlockCollisionComponent;
import org.spout.vanilla.controller.object.moving.Item;
import org.spout.vanilla.data.VanillaData;

/**
 * Controller that is the parent of all entity controllers.
 */
public abstract class VanillaEntityController extends BasicController implements VanillaController {
	private final VanillaControllerType type;
	private final BoundingBox area = new BoundingBox(-0.3F, 0F, -0.3F, 0.3F, 0.8F, 0.3F);
	private static Random rand = new Random();
	// Protocol: last known updated client transform
	private Transform lastClientTransform = new Transform();
	// Tick effects
	private int positionTicks = 0;
	private int velocityTicks = 0;
	// Velocity-related
	private Vector3 velocity = Vector3.ZERO;
	private Vector3 movementSpeed = Vector3.ZERO;
	private Vector3 maxSpeed = new Vector3(0.4, 0.4, 0.4);
	// Block collision handling
	protected BlockCollisionComponent blockCollProcess;
	protected HealthComponent healthProcess;
	protected FireDamageComponent fireDamageProcess;

	protected VanillaEntityController(VanillaControllerType type) {
		super(type);
		this.type = type;
	}

	@Override
	public void onAttached() {
		getParent().setCollision(new CollisionModel(area));
		getParent().getCollision().setStrategy(CollisionStrategy.SOLID);
		data().put(VanillaData.CONTROLLER_TYPE, getType().getMinecraftId());
		this.lastClientTransform = getParent().getTransform();

		healthProcess = registerProcess(new HealthComponent(this, LogicPriority.HIGHEST));
		blockCollProcess = registerProcess(new BlockCollisionComponent(this, LogicPriority.HIGHEST));
		fireDamageProcess = registerProcess(new FireDamageComponent(this, LogicPriority.HIGHEST));

		// Load data
		maxSpeed = data().get(VanillaData.MAX_SPEED, maxSpeed);
		movementSpeed = data().get(VanillaData.MOVEMENT_SPEED, movementSpeed);
		if (data().containsKey(VanillaData.VELOCITY)) {
			velocity = data().get(VanillaData.VELOCITY);
		}
	}

	@Override
	public void onSave() {
		// Load data
		data().put(VanillaData.MAX_SPEED, maxSpeed);
		data().put(VanillaData.MOVEMENT_SPEED, movementSpeed);
		data().put(VanillaData.VELOCITY, velocity);
	}

	@Override
	public void onTick(float dt) {
		if (this.healthProcess.isDying()) {
			this.healthProcess.run();
			return;
		}

		positionTicks++;
		velocityTicks++;

		super.onTick(dt);
	}

	@Override
	public void onCollide(Block block) {
		setVelocity(Vector3.ZERO);
	}

	@Override
	public void onCollide(Entity entity) {
		// push entities apart
		// TODO: Ignore if this entity is a passenger?
		Vector2 diff = entity.getPosition().subtract(this.getParent().getPosition()).toVector2();
		float distance = diff.length();
		if (distance > 0.1f) {
			double factor = Math.min(1f / distance, 1f) / distance * 0.05;
			diff = diff.multiply(factor);
			setVelocity(getVelocity().add(diff.toVector3()));
		}
	}

	@Override
	public void onDeath() {
		for (ItemStack drop : getDrops(getHealth().getLastDamageCause(), getHealth().getLastDamager())) {
			if (drop == null) {
				continue;
			}
			Item item = new Item(drop, Vector3.ZERO);
			getParent().getLastTransform().getPosition().getWorld().createAndSpawnEntity(getParent().getLastTransform().getPosition(), item);
			// TODO: Drop experience
		}
	}

	public void kill() {
		getParent().kill();
	}

	@Override
	public VanillaControllerType getType() {
		return type;
	}

	@Override
	public void callProtocolEvent(ProtocolEvent event) {
		for (Player player : getParent().getWorld().getNearbyPlayers(getParent(), 160)) {
			player.getNetworkSynchronizer().callProtocolEvent(event);
		}
	}

	/**
	 * Gets the fire damage component
	 * 
	 * @return entity fire damage process
	 */
	public FireDamageComponent getFireDamage() {
		return fireDamageProcess;
	}

	/**
	 * Gets the block collision component
	 * 
	 * @return block collision process
	 */
	public BlockCollisionComponent getCollisionComponent() {
		return blockCollProcess;
	}

	/**
	 * Gets the health component
	 * 
	 * @return entity health process
	 */
	public HealthComponent getHealth() {
		return healthProcess;
	}

	/**
	 * Sets the last known transformation known by the clients<br>
	 * This should only be called by the protocol classes
	 * @param transform to set to
	 */
	public void setLastClientTransform(Transform transform) {
		this.lastClientTransform = transform.copy();
	}

	/**
	 * Gets the last known transformation updated to the clients
	 * @return the last known transform by the clients
	 */
	public Transform getLastClientTransform() {
		return this.lastClientTransform;
	}

	/**
	 * Gets the bounding box for the entity that this controller supplies
	 * @return the bounding box for the entity
	 */
	public BoundingBox getBounds() {
		return this.area;
	}

	/**
	 * Tests if a velocity update is needed for this entity<br>
	 * This is called by the entity protocol
	 * @return True if a velocity update is needed
	 */
	public boolean needsVelocityUpdate() {
		return velocityTicks % 5 == 0;
	}

	/**
	 * Tests if a position update is needed for this entity<br>
	 * This is called by the entity protocol
	 * @return True if a position update is needed
	 */
	public boolean needsPositionUpdate() {
		return positionTicks % 30 == 0;
	}

	/**
	 * Gets the current velocity of this controller
	 * @return the velocity
	 */
	public Vector3 getVelocity() {
		return velocity;
	}

	/**
	 * Sets the current velocity for this controller
	 * @param velocity to set to
	 */
	public void setVelocity(Vector3 velocity) {
		if (velocity == null) {
			if (Spout.debugMode()) {
				Spout.getLogger().log(Level.SEVERE, "Velocity of " + this.toString() + " set to null!");
				Spout.getLogger().log(Level.SEVERE, "Report this to http://issues.spout.org");
			}
			velocity = Vector3.ZERO;
		}
		this.velocity = velocity;
	}

	/**
	 * Gets the speed of the controller during the prior movement. This will always be lower than the maximum speed.
	 * @return the moved velocity
	 */
	public Vector3 getMovementSpeed() {
		return movementSpeed;
	}

	/**
	 * Gets the maximum speed this controller is allowed to move at once.
	 * @return the maximum velocity
	 */
	public Vector3 getMaxSpeed() {
		return maxSpeed;
	}

	/**
	 * Sets the maximum speed this controller is allowed to move at once.
	 * @param maxSpeed to set to
	 */
	public void setMaxSpeed(Vector3 maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	/**
	 * Get the drops that Vanilla controllers disperse into the world when un-attached (such as entity death). Children controllers should override this method for their own personal drops.
	 * @param source Source of death
	 * @param lastDamager Controller that killed this controller, can be null if death was caused by natural sources such as drowning or burning
	 * @return the drops to disperse.
	 */
	public Set<ItemStack> getDrops(Source source, VanillaEntityController lastDamager) {
		return new HashSet<ItemStack>();
	}

	// =========================
	// Controller helper methods
	// =========================

	/**
	 * Moves this controller.
	 * @param vect the vector that is applied as the movement.
	 */
	public void move(Vector3 vect) {
		getParent().translate(vect);
	}

	/**
	 * Uses the current velocity and maximum speed to move this entity.
	 */
	public void move() {
		movementSpeed = MathHelper.min(velocity, maxSpeed);
		move(movementSpeed);
	}

	/**
	 * Moves this controller
	 * @param x x-axis to move the controller along
	 * @param y y-axis to move the controller along
	 * @param z z-axis to move the controller along
	 */
	public void move(float x, float y, float z) {
		move(new Vector3(x, y, z));
	}

	/**
	 * Rotates the controller
	 * @param rot the quaternion that is applied as the rotation.
	 */
	public void rotate(Quaternion rot) {
		getParent().rotate(rot);
	}

	/**
	 * Rotates the controller
	 * @param degrees the angle of which to do rotation.
	 * @param x x-axis to rotate the controller along
	 * @param y y-axis to rotate the controller along
	 * @param z z-axis to rotate the controller along
	 */
	public void rotate(float degrees, float x, float y, float z) {
		getParent().rotate(degrees, x, y, z);
	}

	/**
	 * Sets the yaw angle for this controller
	 * @param angle to set to
	 */
	public void yaw(float angle) {
		getParent().yaw(angle);
	}

	/**
	 * Sets the pitch angle for this controller
	 * @param angle to set to
	 */
	public void pitch(float angle) {
		getParent().pitch(angle);
	}

	/**
	 * Rolls this controller along an angle.
	 * @param angle the angle in-which to roll
	 */
	public void roll(float angle) {
		getParent().roll(angle);
	}

	/**
	 * Gets the position of this controller at the start of this tick
	 * @return previous position
	 */
	public Vector3 getPreviousPosition() {
		return getParent().getLastTransform().getPosition();
	}

	/**
	 * Gets the rotation of this controller at the start of this tick
	 * @return previous rotation
	 */
	public Quaternion getPreviousRotation() {
		return getParent().getLastTransform().getRotation();
	}

	/**
	 * If a child controller needs a random number for anything, they should call this method. This eliminates needless random objects created all the time.
	 * @return random object.
	 */
	public Random getRandom() {
		return rand;
	}
}