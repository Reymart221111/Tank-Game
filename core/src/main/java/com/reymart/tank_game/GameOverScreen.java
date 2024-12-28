// GameOverScreen.java
package com.reymart.tank_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class GameOverScreen implements Dispose {
    private BitmapFont font;
    private SpriteBatch batch;
    private boolean isActive = false;
    private OrthographicCamera camera;
    private int finalScore;

    public GameOverScreen(SpriteBatch batch, OrthographicCamera camera) {
        this.batch = batch;
        this.camera = camera;
        initializeFont();
    }

    private void initializeFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 48;
        parameter.color = Color.RED;
        parameter.borderWidth = 3;
        parameter.borderColor = Color.BLACK;
        font = generator.generateFont(parameter);
        generator.dispose();
    }

    public void show(int score) {
        this.isActive = true;
        this.finalScore = score;
    }

    public void hide() {
        this.isActive = false;
    }

    public boolean isActive() {
        return isActive;
    }

    public void render() {
        if (!isActive) return;

        float cameraX = camera.position.x;
        float cameraY = camera.position.y;
        float offsetX = camera.viewportWidth * camera.zoom / 2;
        float offsetY = camera.viewportHeight * camera.zoom / 2;

        // Position text relative to camera center
        float gameOverTextX = cameraX - 150; // Adjust these values to center the text
        float gameOverTextY = cameraY + 50;
        float scoreTextX = cameraX - 100;
        float scoreTextY = cameraY;
        float restartTextX = cameraX - 200;
        float restartTextY = cameraY - 50;

        batch.begin();
        font.draw(batch, "GAME OVER!!", gameOverTextX, gameOverTextY);
        font.draw(batch, "Score: " + finalScore, scoreTextX, scoreTextY);
        font.draw(batch, "Press ENTER to restart!!", restartTextX, restartTextY);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            TankGame.getInstance().restartGame();
        }
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
    }
}
