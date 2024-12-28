package com.reymart.tank_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

public class PlayerScoreUI implements Disposable {
    private BitmapFont font;
    private SpriteBatch batch;
    private PlayerTank playerTank;
    private int score = 0;

    public PlayerScoreUI(PlayerTank playerTank, SpriteBatch batch) {
        this.playerTank = playerTank;
        this.batch = batch;
        initializeFont();
    }

    private void initializeFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 20;
        parameter.color = Color.WHITE;
        parameter.borderWidth = 2;
        parameter.borderColor = Color.BLACK;
        font = generator.generateFont(parameter);
        generator.dispose();
    }

    public void incrementScore() {
        score++;
    }

    public void render() {
        batch.begin();
        // Position the score above the health bar
        String scoreText = "Score: " + score;
        font.draw(batch, scoreText,
            playerTank.getX() - 30,
            playerTank.getY() + 120);
        batch.end();
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
    }

    public int getScore() {
        return score;
    }
}
