package com.reymart.tank_game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class EnemyTankHealthBar extends Actor implements  Dispose {
    private EnemyTank enemyTank;
    private float maxHealth = 100f;
    private float currentHealth;

    private static final float BAR_WIDTH = 50f;
    private static final float BAR_HEIGHT = 5f;
    private static final float BAR_OFFSET_Y = 10f; // Offset above the tank

    private ShapeRenderer shapeRenderer;

    public EnemyTankHealthBar(EnemyTank enemyTank, float maxHealth) {
        this.enemyTank = enemyTank;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;

        this.shapeRenderer = new ShapeRenderer();
    }

    public void updateHealth(float newHealth) {
        this.currentHealth = Math.max(0, Math.min(newHealth, maxHealth)); // Clamp health between 0 and maxHealth
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.end(); // End the batch to draw shapes

        shapeRenderer.setProjectionMatrix(getStage().getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw background (red bar)
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(
            enemyTank.getX() + enemyTank.getWidth() / 2 - BAR_WIDTH / 2,
            enemyTank.getY() + enemyTank.getHeight() + BAR_OFFSET_Y,
            BAR_WIDTH,
            BAR_HEIGHT
        );

        // Draw health (green bar)
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(
            enemyTank.getX() + enemyTank.getWidth() / 2 - BAR_WIDTH / 2,
            enemyTank.getY() + enemyTank.getHeight() + BAR_OFFSET_Y,
            BAR_WIDTH * (currentHealth / maxHealth), // Scale width based on health
            BAR_HEIGHT
        );

        shapeRenderer.end();
        batch.begin(); // Resume the batch
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
