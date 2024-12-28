package com.reymart.tank_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class PlayerTank extends Actor implements Dispose {
    private static final float WORLD_WIDTH = 5000f;
    private static final float WORLD_HEIGHT = 5000f;

    private AnimationManager movingAnimation;
    private AnimationManager idleAnimation;
    private AnimationManager turretAnimation;
    private AnimationManager shellAnimation;

    private float moveSpeed = 300f;
    private float rotationSpeed = 180f;
    private float bodyRotation = 0f;
    private float turretRotation = 0f;
    private boolean isMoving = false;

    private Stage stage;
    private Rectangle hitbox;
    private float shootCooldown = 0f;
    private static final float SHOOT_INTERVAL = 0.1f;

    private int health = 150;

    // Sound effects
    private Sound moveSound;
    private Sound turretRotationSound;
    private Sound fireSound;
    private Sound hitSound;
    private Sound idleSound;
    private boolean isPlayingMoveSound = false;
    private boolean isPlayingIdleSound = false;

    public PlayerTank(AnimationManager movingAnimation,
                      AnimationManager idleAnimation,
                      AnimationManager turretAnimation,
                      AnimationManager shellAnimation,
                      Stage stage) {
        this.movingAnimation = movingAnimation;
        this.idleAnimation = idleAnimation;
        this.turretAnimation = turretAnimation;
        this.shellAnimation = shellAnimation;
        this.stage = stage;

        setSize(64, 64);
        setPosition(750, 450);

        hitbox = new Rectangle(
            getX() + 10,
            getY() + 10,
            getWidth() - 20,
            getHeight() - 20
        );

        // Initialize sound effects
        moveSound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank-moving.mp3"));
        turretRotationSound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank-turret-rotate.mp3"));
        fireSound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank-fire.wav"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank-hit.mp3"));
        idleSound = Gdx.audio.newSound(Gdx.files.internal("sounds/tank-idle-engine.mp3"));
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void reduceHealth(int amount) {
        this.health -= amount;
        if (this.health <= 0) {
            this.health = 0;
            handleGameOver();
        }
    }

    private void handleGameOver() {
        System.out.println("Game Over! Player tank destroyed.");
        // Get final score before removing tank
        int finalScore = TankGame.getInstance().getPlayerScoreUI().getScore();
        // Show game over screen with final score
        TankGame.getInstance().showGameOver(finalScore);
        stage.getActors().removeValue(this, true);
    }

    public void onHit() {
        System.out.println("Player hit by shell!");
        hitSound.play(9.0f);
        reduceHealth(20);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (shootCooldown > 0) {
            shootCooldown -= delta;
        }

        float radians = bodyRotation * MathUtils.degreesToRadians;
        float moveX = 0;
        float moveY = 0;
        boolean moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveX = moveSpeed * MathUtils.cos(radians);
            moveY = moveSpeed * MathUtils.sin(radians);
            moving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveX = -moveSpeed * MathUtils.cos(radians);
            moveY = -moveSpeed * MathUtils.sin(radians);
            moving = true;
        }

        float newX = getX() + moveX * delta;
        float newY = getY() + moveY * delta;

        if (checkCollisionAvoidance(newX, newY)) {
            newX = MathUtils.clamp(newX, 0, WORLD_WIDTH - getWidth());
            newY = MathUtils.clamp(newY, 0, WORLD_HEIGHT - getHeight());

            setX(newX);
            setY(newY);
            hitbox.setPosition(newX + 10, newY + 10);
        }

        if (moving) {
            if (!isPlayingMoveSound) {
                moveSound.loop(0.4f);
                idleSound.stop();
                isPlayingMoveSound = true;
                isPlayingIdleSound = false;
            }
        } else {
            moveSound.stop();
            isPlayingMoveSound = false;
            if (!isPlayingIdleSound) {
                idleSound.loop(1f);
                isPlayingIdleSound = true;
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            bodyRotation += rotationSpeed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            bodyRotation -= rotationSpeed * delta;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            turretRotation += rotationSpeed * delta;
            turretRotationSound.play();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            turretRotation -= rotationSpeed * delta;
            turretRotationSound.play();
        }

        isMoving = moveX != 0 || moveY != 0 || Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && shootCooldown <= 0) {
            shootShell();
            shootCooldown = SHOOT_INTERVAL;
            fireSound.play(2.0f);
        }
    }

    private boolean checkCollisionAvoidance(float newX, float newY) {
        final float COLLISION_BUFFER = 40f;

        Rectangle proposedHitbox = new Rectangle(
            newX + 10 - COLLISION_BUFFER,
            newY + 10 - COLLISION_BUFFER,
            getWidth() - 20 + COLLISION_BUFFER * 2,
            getHeight() - 20 + COLLISION_BUFFER * 2
        );

        for (Actor actor : stage.getActors()) {
            if (actor instanceof EnemyTank) {
                Rectangle otherHitbox = ((EnemyTank) actor).getHitbox();
                if (proposedHitbox.overlaps(otherHitbox)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void shootShell() {
        float radians = turretRotation * MathUtils.degreesToRadians;

        float turretLength = 15f;
        float startX = getX() + getWidth() / 2 + (getWidth() / 2 + turretLength) * MathUtils.cos(radians);
        float startY = getY() + getHeight() / 2 + (getHeight() / 2 + turretLength) * MathUtils.sin(radians);

        Shell shell = new Shell(shellAnimation, startX, startY, turretRotation, stage, this);
        shell.setScale(0.8f);
        shell.setMaxRange(1500f);
        stage.addActor(shell);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        AnimationManager currentAnimation = isMoving ? movingAnimation : idleAnimation;

        TextureRegion bodyFrame = currentAnimation.getCurrentFrame(Gdx.graphics.getDeltaTime());
        batch.draw(bodyFrame,
            getX(), getY(),
            getWidth() / 2f, getHeight() / 2f,
            getWidth(), getHeight(),
            1.8f, 1.4f,
            bodyRotation
        );

        TextureRegion turretFrame = turretAnimation.getCurrentFrame(Gdx.graphics.getDeltaTime());
        float tankCenterX = getX() + getWidth() / 2f;
        float tankCenterY = getY() + getHeight() / 2f;

        float turretX = tankCenterX - turretFrame.getRegionWidth() / 2f;
        float turretY = tankCenterY - turretFrame.getRegionHeight() / 2f;

        batch.draw(turretFrame,
            turretX,
            turretY,
            turretFrame.getRegionWidth() / 2f,
            turretFrame.getRegionHeight() / 2f,
            turretFrame.getRegionWidth(),
            turretFrame.getRegionHeight(),
            1.8f, 1.f,
            turretRotation
        );
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    @Override
    public void dispose() {
        if (movingAnimation != null) movingAnimation.dispose();
        if (idleAnimation != null) idleAnimation.dispose();
        if (turretAnimation != null) turretAnimation.dispose();
        if (shellAnimation != null) shellAnimation.dispose();
        if (moveSound != null) moveSound.dispose();
        if (turretRotationSound != null) turretRotationSound.dispose();
        if (fireSound != null) fireSound.dispose();
        if (hitSound != null) hitSound.dispose();
        if (idleSound != null) idleSound.dispose();
    }
}
