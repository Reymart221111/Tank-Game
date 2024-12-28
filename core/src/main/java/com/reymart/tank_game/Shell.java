package com.reymart.tank_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class Shell extends Actor implements Dispose {
    private AnimationManager shellAnimation;
    private Vector2 velocity;
    private float speed = 3000f; // Shell speed
    private float rotation;
    private float scale = 1.5f; // Default scale value

    private float maxRange = 1000f; // Maximum range in pixels
    private float traveledDistance = 0f;
    private Vector2 startPosition;

    private Stage stage;
    private Actor shooter; // Reference to the tank that fired this shell
    private Rectangle hitbox;
    private Actor owner; // Can be PlayerTank or EnemyTank

    public Shell(AnimationManager shellAnimation, float startX, float startY, float rotation, Stage stage, Actor shooter) {
        this.shellAnimation = shellAnimation;
        this.owner = owner;
        this.rotation = rotation;
        this.stage = stage;
        this.shooter = shooter;

        setSize(64f, 64f);
        setPosition(startX, startY);

        startPosition = new Vector2(startX, startY);

        float radians = rotation * MathUtils.degreesToRadians;
        velocity = new Vector2(-speed * MathUtils.cos(radians), -speed * MathUtils.sin(radians));

        hitbox = new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        // Update shell position
        float previousX = getX();
        float previousY = getY();

        setX(getX() + velocity.x * delta);
        setY(getY() + velocity.y * delta);

        hitbox.setPosition(getX(), getY());

        float deltaX = getX() - previousX;
        float deltaY = getY() - previousY;
        traveledDistance += Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // Remove shell if it exceeds max range or collides
        if (traveledDistance >= maxRange || checkCollision()) {
            remove();
        }
    }

    private boolean checkCollision() {
        for (Actor actor : stage.getActors()) {
            if (actor == shooter) {
                // Ignore the tank that fired this shell
                continue;
            }

            if (actor instanceof PlayerTank) {
                // Check collision with the player
                Rectangle playerHitbox = ((PlayerTank) actor).getHitbox();
                if (hitbox.overlaps(playerHitbox)) {
                    ((PlayerTank) actor).onHit(); // Trigger a method to handle the hit
                    return true;
                }
            }

            if (actor instanceof EnemyTank) {
                // Check collision with enemy tanks
                Rectangle enemyHitbox = ((EnemyTank) actor).getHitbox();
                if (hitbox.overlaps(enemyHitbox)) {
                    ((EnemyTank) actor).onHit(); // Trigger a method to handle the hit for EnemyTank
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void draw(Batch batch, float parentAlpha) {
        TextureRegion shellFrame = shellAnimation.getCurrentFrame(Gdx.graphics.getDeltaTime());

        batch.draw(shellFrame,
            getX(), getY(),
            shellFrame.getRegionWidth() / 2f,
            shellFrame.getRegionHeight() / 2f,
            shellFrame.getRegionWidth(),
            shellFrame.getRegionHeight(),
            scale, scale,
            rotation
        );
    }

    public void setMaxRange(float range) {
        this.maxRange = range;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
    public Actor getOwner() {
        return owner;
    }

    @Override
    public void dispose() {
        if (shellAnimation != null) shellAnimation.dispose();
    }
}
