package com.reymart.tank_game;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class PlayerHealthUI {
    private final PlayerTank playerTank;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final ShapeRenderer shapeRenderer;

    public PlayerHealthUI(PlayerTank playerTank, SpriteBatch batch) {
        this.playerTank = playerTank;
        this.batch = batch;
        this.font = new BitmapFont(); // Default font
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render() {
        float tankX = playerTank.getX();
        float tankY = playerTank.getY();
        float healthBarWidth = 50f; // Width of the health bar
        float healthBarHeight = 8f; // Height of the health bar
        float healthBarX = tankX + playerTank.getWidth() / 2f - healthBarWidth / 2f; // Centered above the tank
        float healthBarY = tankY + playerTank.getHeight() + 10f; // Slightly above the tank

        // Calculate the health bar fill based on the player's health
        float healthPercentage = Math.max(0, playerTank.getHealth() / 100f);
        float healthFillWidth = healthBarWidth * healthPercentage;

        // Draw the health bar (background and foreground)
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background (black bar)
        shapeRenderer.setColor(0, 0, 0, 1);
        shapeRenderer.rect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // Foreground (health fill, green -> red based on health)
        shapeRenderer.setColor(1 - healthPercentage, healthPercentage, 0, 1); // Gradient from red to green
        shapeRenderer.rect(healthBarX, healthBarY, healthFillWidth, healthBarHeight);

        shapeRenderer.end();

        // Optionally, draw health text
        batch.begin();
        font.draw(batch, "HP: " + playerTank.getHealth(), healthBarX, healthBarY + healthBarHeight + 15);
        batch.end();
    }

    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
    }
}
