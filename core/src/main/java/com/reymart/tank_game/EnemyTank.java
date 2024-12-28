package com.reymart.tank_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import java.util.List;

public class EnemyTank extends Actor implements Dispose {
    private static final float WORLD_WIDTH = 5000f;
    private static final float WORLD_HEIGHT = 5000f;

    // New constants for smoother rotation
    private static final float BODY_ROTATION_SPEED = 0.03f; // Lower value for smoother rotation
    private static final float WANDER_ROTATION_SPEED = 0.5f; // Even smoother rotation for wandering

    private AnimationManager movingAnimation;
    private AnimationManager idleAnimation;
    private AnimationManager turretAnimation;
    private AnimationManager shellAnimation;

    private float moveSpeed = 150f;
    private float rotationSpeed = 120f;
    private float bodyRotation = 0f;
    private float turretRotation = 0f;
    private boolean isMoving = false;

    // Sound effects
    private Sound tankFireSound;
    private Sound tankMovementSound;
    private Sound tankHitSound;
    private Sound tankExplosionSound;
    private long movementSoundId; // To control looping movement sound
    private boolean isPlayingMovementSound;

    private Stage stage;
    private PlayerTank playerTank;
    private float turretRotationOriginX = 2f;
    private float turretRotationOriginY = 2f;

    private static final float DETECTION_RANGE = 1000f;
    private static final float STOP_DISTANCE = 500f;
    private static final float SHOOT_INTERVAL = 2f;
    private static final float TURRET_ROTATION_SPEED = 2f;
    private static final float COLLISION_BUFFER = 60f;

    private static final float COLLISION_AVOIDANCE_DISTANCE = 100f; // Distance at which to start avoiding collision
    private boolean canMove = true;

    private boolean isColliding = false;
    private float collisionCooldown = 0f;
    private static final float COLLISION_COOLDOWN_DURATION = 1f; //

    private float wanderTimer = 0f;
    private float wanderInterval = 3f;
    private Vector2 wanderDirection = new Vector2();

    private float shootCooldown = 0f;
    private Rectangle hitbox;

    private float maxHealth = 100f;
    private float currentHealth = maxHealth;
    private EnemyTankHealthBar healthBar;

    private AnimationManager explosionAnimation;
    private boolean isExploding = false;
    private float explosionTimer = 0f;
    private static final float EXPLOSION_DURATION = 1f;


    public EnemyTank(AnimationManager movingAnimation,
                     AnimationManager idleAnimation,
                     AnimationManager turretAnimation,
                     AnimationManager shellAnimation,
                     AnimationManager explosionAnimation, // New parameter
                     Stage stage,
                     PlayerTank playerTank,
                     float x,
                     float y) {
        this.movingAnimation = movingAnimation;
        this.idleAnimation = idleAnimation;
        this.turretAnimation = turretAnimation;
        this.shellAnimation = shellAnimation;
        this.explosionAnimation = explosionAnimation; // Store explosion animation
        this.stage = stage;
        this.playerTank = playerTank;
        this.healthBar = new EnemyTankHealthBar(this, maxHealth);
        stage.addActor(healthBar);

        setSize(64, 64);
        setPosition(x, y);

        hitbox = new Rectangle(getX() + 10, getY() + 10, getWidth() - 20, getHeight() - 20);

        tankFireSound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank-fire.wav"));
        tankHitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank-hit.mp3"));
        tankExplosionSound = Gdx.audio.newSound(Gdx.files.internal("sounds/explosion.wav"));
        isPlayingMovementSound = false;

    }


    public void reduceHealth(float amount) {
        if (isExploding) return; // Prevent further damage during explosion

        this.currentHealth -= amount;
        this.healthBar.updateHealth(this.currentHealth);

        // Play hit sound when taking damage
        tankHitSound.play(0.6f); // Adjust volume as needed



        if (this.currentHealth <= 0) {
            this.currentHealth = 0;
            // Start explosion sequence
            isExploding = true;
            explosionTimer = 0f;
            explosionAnimation.resetStateTime(); // Reset state time instead of reset

            // Play explosion sound
            tankExplosionSound.play(0.7f); // Adjust volume as needed
            // Increment the player's score using the singleton instance
            TankGame.getInstance().getPlayerScoreUI().incrementScore();
        }
    }


    public void onHit() {
        System.out.println("Enemy hit by shell!");
        reduceHealth(30); // Example damage value
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (shootCooldown > 0) {
            shootCooldown -= delta;
        }

        // Handle collision cooldown
        if (!canMove) {
            collisionCooldown -= delta;
            if (collisionCooldown <= 0) {
                // Check if we're still colliding
                if (!checkCurrentCollisions()) {
                    canMove = true;
                    collisionCooldown = 0f;
                } else {
                    // Reset cooldown if still colliding
                    collisionCooldown = COLLISION_COOLDOWN_DURATION;
                }
            }
        }

        float distanceToPlayer = Vector2.dst(getX(), getY(), playerTank.getX(), playerTank.getY());

        if (distanceToPlayer <= DETECTION_RANGE) {
            if (distanceToPlayer > STOP_DISTANCE) {
                if (canMove) {
                    moveTowardsPlayerWithCollisionAvoidance(delta);

                    // Shoot while moving towards the player
                    if (shootCooldown <= 0) {
                        attemptToShootAtPlayer();
                    }
                } else {
                    // Stop moving if the tank is colliding or near the collision buffer
                    bodyRotation = MathUtils.lerpAngleDeg(bodyRotation, bodyRotation, BODY_ROTATION_SPEED);
                }
            } else {
                stopAndAimAtPlayer(delta);
            }
        } else {
            performWanderMovement(delta);
        }


        // Handle explosion animation
        if (isExploding) {
            explosionTimer += delta;
            if (explosionTimer >= EXPLOSION_DURATION) {
                // Remove tank and health bar when explosion is complete
                this.remove();
                this.healthBar.remove();
            }
        }

        isMoving = wanderDirection.len() > 0 && canMove;
    }

    private void attemptToShootAtPlayer() {
        float targetX = playerTank.getX();
        float targetY = playerTank.getY();

        float angleToPlayer = MathUtils.atan2(
            targetY - getY(),
            targetX - getX()
        ) * MathUtils.radiansToDegrees;

        // Adjust turret rotation towards the player while moving
        turretRotation = MathUtils.lerpAngleDeg(turretRotation, angleToPlayer, TURRET_ROTATION_SPEED);

        // Shoot the shell
        shootShell();
        shootCooldown = SHOOT_INTERVAL;
    }

    private void moveTowardsPlayerWithCollisionAvoidance(float delta) {
        float targetX = playerTank.getX();
        float targetY = playerTank.getY();

        float angleToPLayer = MathUtils.atan2(
            targetY - getY(),
            targetX - getX()
        ) * MathUtils.radiansToDegrees;

        bodyRotation = MathUtils.lerpAngleDeg(bodyRotation, angleToPLayer, BODY_ROTATION_SPEED);

        float radians = bodyRotation * MathUtils.degreesToRadians;
        float moveX = moveSpeed * MathUtils.cos(radians);
        float moveY = moveSpeed * MathUtils.sin(radians);

        float newX = getX() + moveX * delta;
        float newY = getY() + moveY * delta;

        boolean canMove = checkCollisionAvoidance(newX, newY);

        if (canMove) {
            newX = MathUtils.clamp(newX, 0, WORLD_WIDTH - getWidth());
            newY = MathUtils.clamp(newY, 0, WORLD_HEIGHT - getHeight());

            setX(newX);
            setY(newY);
            hitbox.setPosition(newX + 10, newY + 10);
            this.canMove = true;
        } else {
            resolveCollision(newX, newY);
            this.canMove = false;
            this.collisionCooldown = COLLISION_COOLDOWN_DURATION;
        }
    }

    private boolean checkCurrentCollisions() {
        Rectangle proposedHitbox = new Rectangle(
            getX() + 10 - COLLISION_BUFFER, getY() + 10 - COLLISION_BUFFER,
            getWidth() - 20 + COLLISION_BUFFER * 2, getHeight() - 20 + COLLISION_BUFFER * 2
        );

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyTank && actor != this) {
                Rectangle otherHitbox = ((EnemyTank) actor).getHitbox();
                if (proposedHitbox.overlaps(otherHitbox)) {
                    return true; // Still colliding
                }
            } else if (actor instanceof PlayerTank) {
                Rectangle otherHitbox = ((PlayerTank) actor).getHitbox();
                if (proposedHitbox.overlaps(otherHitbox)) {
                    return true; // Still colliding
                }
            }
        }
        return false;
    }


    private boolean checkCollisionAvoidance(float newX, float newY) {
        Rectangle proposedHitbox = new Rectangle(
            newX + 10 - COLLISION_BUFFER, newY + 10 - COLLISION_BUFFER,
            getWidth() - 20 + COLLISION_BUFFER * 2, getHeight() - 20 + COLLISION_BUFFER * 2
        );

        boolean isNearCollision = false;
        for (Actor actor : stage.getActors()) {
            // Check for collisions with other enemy tanks
            if (actor instanceof EnemyTank && actor != this) {
                Rectangle otherHitbox = ((EnemyTank) actor).getHitbox();
                if (proposedHitbox.overlaps(otherHitbox)) {
                    isNearCollision = true;
                    break;
                }
            }
            // Check for collisions with the player tank
            else if (actor instanceof PlayerTank) {
                Rectangle otherHitbox = ((PlayerTank) actor).getHitbox();
                if (proposedHitbox.overlaps(otherHitbox)) {
                    isNearCollision = true;
                    break;
                }
            }
        }

        if (isNearCollision) {
            isColliding = true;
            return false;
        } else {
            isColliding = false;
            return true;
        }
    }

    private void resolveCollision(float newX, float newY) {
        Rectangle proposedHitbox = new Rectangle(
            newX + 10 - COLLISION_BUFFER, newY + 10 - COLLISION_BUFFER,
            getWidth() - 20 + COLLISION_BUFFER * 2, getHeight() - 20 + COLLISION_BUFFER * 2
        );

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyTank && actor != this) {
                Rectangle otherHitbox = ((EnemyTank) actor).getHitbox();
                if (proposedHitbox.overlaps(otherHitbox)) {
                    // Resolve the collision by separating the tanks along the normal of the collision
                    float overlapX = Math.min(getX() + getWidth() - actor.getX(), actor.getX() + actor.getWidth() - getX());
                    float overlapY = Math.min(getY() + getHeight() - actor.getY(), actor.getY() + actor.getHeight() - getY());

                    if (overlapX < overlapY) {
                        if (getX() < actor.getX()) {
                            setX(actor.getX() - getWidth() - COLLISION_BUFFER);
                        } else {
                            setX(actor.getX() + actor.getWidth() + COLLISION_BUFFER);
                        }
                    } else {
                        if (getY() < actor.getY()) {
                            setY(actor.getY() - getHeight() - COLLISION_BUFFER);
                        } else {
                            setY(actor.getY() + actor.getHeight() + COLLISION_BUFFER);
                        }
                    }

                    hitbox.setPosition(getX() + 10, getY() + 10);
                    ((EnemyTank) actor).hitbox.setPosition(actor.getX() + 10, actor.getY() + 10);
                    return;
                }
            }
        }
    }

    private void stopAndAimAtPlayer(float delta) {
        float targetX = playerTank.getX();
        float targetY = playerTank.getY();

        float angleToPlayer = MathUtils.atan2(
            targetY - getY(),
            targetX - getX()
        ) * MathUtils.radiansToDegrees + 180f;

        angleToPlayer = (angleToPlayer + 360f) % 360f;

        bodyRotation = MathUtils.lerpAngleDeg(bodyRotation, angleToPlayer, BODY_ROTATION_SPEED);

        turretRotation = MathUtils.lerpAngleDeg(turretRotation, angleToPlayer, TURRET_ROTATION_SPEED * delta);

        if (shootCooldown <= 0) {
            shootShell();
            shootCooldown = SHOOT_INTERVAL;
        }
    }

    private void performWanderMovement(float delta) {
        wanderTimer += delta;
        if (wanderTimer >= wanderInterval) {
            updateWanderDirection();
            wanderTimer = 0f;
        }

        float radians = bodyRotation * MathUtils.degreesToRadians;
        float moveX = moveSpeed * wanderDirection.x;
        float moveY = moveSpeed * wanderDirection.y;

        float newX = getX() + moveX * delta;
        float newY = getY() + moveY * delta;

        newX = MathUtils.clamp(newX, 0, WORLD_WIDTH - getWidth());
        newY = MathUtils.clamp(newY, 0, WORLD_HEIGHT - getHeight());

        setX(newX);
        setY(newY);
        hitbox.setPosition(newX + 10, newY + 10);
    }

    private void updateWanderDirection() {
        float randomAngle = MathUtils.random(0f, 360f);
        wanderDirection.set(
            MathUtils.cos(randomAngle * MathUtils.degreesToRadians),
            MathUtils.sin(randomAngle * MathUtils.degreesToRadians)
        );

        bodyRotation = MathUtils.lerpAngleDeg(bodyRotation, randomAngle, WANDER_ROTATION_SPEED);
    }

    private void shootShell() {
        float radians = turretRotation * MathUtils.degreesToRadians;

        float turretLength = 0f;
        float startX = getX() + getWidth() / 2 +
            (getWidth() / 2 + turretLength) * MathUtils.cos((float) (radians - Math.PI/2));
        float startY = getY() + getHeight() / 2 +
            (getHeight() / 2 + turretLength) * MathUtils.sin((float) (radians - Math.PI/2));

        Shell shell = new Shell(shellAnimation, startX, startY, turretRotation, stage, this);
        shell.setScale(0.8f);
        shell.setMaxRange(1500f);
        stage.addActor(shell);

        tankFireSound.play(1f);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // If exploding, draw explosion animation
        if (isExploding) {
            TextureRegion explosionFrame = explosionAnimation.getCurrentFrame(Gdx.graphics.getDeltaTime());
            batch.draw(explosionFrame,
                getX(), getY(),
                getWidth() / 2f, getHeight() / 2f,
                getWidth(), getHeight(),
                1.6f, 1.2f,
                bodyRotation
            );
            return;
        }

        // Existing drawing logic for tank body and turret
        AnimationManager currentAnimation = isMoving ? movingAnimation : idleAnimation;

        TextureRegion bodyFrame = currentAnimation.getCurrentFrame(Gdx.graphics.getDeltaTime());
        batch.draw(bodyFrame,
            getX(), getY(),
            getWidth() / 2f, getHeight() / 2f,
            getWidth(), getHeight(),1.6f, 1.2f,
            bodyRotation
        );

        TextureRegion turretFrame = turretAnimation.getCurrentFrame(Gdx.graphics.getDeltaTime());
        float tankCenterX = getX() + getWidth() / turretRotationOriginX;
        float tankCenterY = getY() + getHeight() / turretRotationOriginY;

        float turretX = tankCenterX - turretFrame.getRegionWidth() / 2f;
        float turretY = tankCenterY - turretFrame.getRegionHeight() / 2f;

        batch.draw(turretFrame,
            turretX,
            turretY,
            turretFrame.getRegionWidth() / 2f,
            turretFrame.getRegionHeight() / 2f,
            turretFrame.getRegionWidth(),
            turretFrame.getRegionHeight(),
            1.4f, 1.1f,
            turretRotation
        );
    }


    public Rectangle getHitbox() {
        return hitbox;
    }

    public boolean overlaps(Rectangle other) {
        return hitbox.overlaps(other);
    }

    @Override
    public void dispose() {
        if (movingAnimation != null) movingAnimation.dispose();
        if (idleAnimation != null) idleAnimation.dispose();
        if (turretAnimation != null) turretAnimation.dispose();
        if (shellAnimation != null) shellAnimation.dispose();
        if (explosionAnimation != null) explosionAnimation.dispose(); // Dispose explosion animation

        if (tankFireSound != null) tankFireSound.dispose();
        if (tankMovementSound != null) tankMovementSound.dispose();
        if (tankHitSound != null) tankHitSound.dispose();
        if (tankExplosionSound != null) tankExplosionSound.dispose();
    }
}
