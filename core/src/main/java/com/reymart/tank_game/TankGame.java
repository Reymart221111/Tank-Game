// TankGame.java
package com.reymart.tank_game;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class TankGame extends InputAdapter implements ApplicationListener {
    private static final float WORLD_WIDTH = 5000f;
    private static final float WORLD_HEIGHT = 5000f;
    private static final float CAMERA_LERP_FACTOR = 0.1f;
    private static final float CAMERA_ZOOM_FACTOR = 10000f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private Stage stage;
    private PlayerTank playerTank;
    private PlayerHealthUI playerHealthUI;
    private TextureAtlas backgroundAtlas;
    private EnemyTank[] enemyTanks;

    private static final float SPAWN_INTERVAL = 3f;
    private float timeSinceLastSpawn = 0;

    private PlayerScoreUI playerScoreUI;
    private static TankGame instance;

    private GameOverScreen gameOverScreen;
    private boolean isGameOver = false;

    private static final int MAX_ENEMY_TANKS = 30;
    private Array<EnemyTank> activeEnemyTanks;

    @Override
    public void create() {
        instance = this;
        activeEnemyTanks = new Array<>();
        camera = new OrthographicCamera(WORLD_WIDTH / CAMERA_ZOOM_FACTOR, WORLD_HEIGHT / CAMERA_ZOOM_FACTOR);
        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
        viewport = new ExtendViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        batch = new SpriteBatch();
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(this);

        backgroundAtlas = new TextureAtlas(Gdx.files.internal("atlas/world.atlas"));

        createPlayerTank();
        createPlayerHealthUI();
        createPlayerScoreUI();
        createEnemyTanks();
        gameOverScreen = new GameOverScreen(batch, camera);
    }

    public void removeEnemyTank(EnemyTank tank) {
        activeEnemyTanks.removeValue(tank, true);
    }

    public void showGameOver(int finalScore) {
        isGameOver = true;
        gameOverScreen.show(finalScore);
    }

    public void restartGame() {
        stage.clear();
        activeEnemyTanks.clear();
        isGameOver = false;
        gameOverScreen.hide();
        createPlayerTank();
        createPlayerHealthUI();
        createPlayerScoreUI();
        createEnemyTanks();
    }

    private void createPlayerScoreUI() {
        playerScoreUI = new PlayerScoreUI(playerTank, batch);
    }

    public static TankGame getInstance() {
        return instance;
    }

    public PlayerScoreUI getPlayerScoreUI() {
        return playerScoreUI;
    }

    private void createPlayerTank() {
        TextureAtlas tankMovingAtlas = new TextureAtlas(Gdx.files.internal("atlas/tank_moving.atlas"));
        TextureAtlas tankIdleAtlas = new TextureAtlas(Gdx.files.internal("atlas/tank_idle.atlas"));
        TextureAtlas tankTurretAtlas = new TextureAtlas(Gdx.files.internal("atlas/tank_turret.atlas"));
        TextureAtlas shellAtlas = new TextureAtlas(Gdx.files.internal("atlas/tank_shell.atlas"));

        AnimationManager movingAnimation = new AnimationManager(tankMovingAtlas, Animation.PlayMode.LOOP);
        AnimationManager idleAnimation = new AnimationManager(tankIdleAtlas, Animation.PlayMode.LOOP);
        AnimationManager turretAnimation = new AnimationManager(tankTurretAtlas, Animation.PlayMode.LOOP);
        AnimationManager shellAnimation = new AnimationManager(shellAtlas, Animation.PlayMode.LOOP);

        playerTank = new PlayerTank(movingAnimation, idleAnimation, turretAnimation, shellAnimation, stage);
        stage.addActor(playerTank);
    }

    private void createPlayerHealthUI() {
        playerHealthUI = new PlayerHealthUI(playerTank, batch);
    }

    private void createEnemyTanks() {
        TextureAtlas enemyTankMovingAtlas = new TextureAtlas(Gdx.files.internal("atlas/enemy_tank_moving.atlas"));
        TextureAtlas enemyTankIdleAtlas = new TextureAtlas(Gdx.files.internal("atlas/enemy_tank_idle.atlas"));
        TextureAtlas enemyTankTurretAtlas = new TextureAtlas(Gdx.files.internal("atlas/enemy_tank_turret.atlas"));
        TextureAtlas shellAtlas = new TextureAtlas(Gdx.files.internal("atlas/tank_shell.atlas"));
        TextureAtlas explosionAtlas = new TextureAtlas(Gdx.files.internal("atlas/explosion.atlas"));

        AnimationManager movingAnimation = new AnimationManager(enemyTankMovingAtlas, Animation.PlayMode.LOOP);
        AnimationManager idleAnimation = new AnimationManager(enemyTankIdleAtlas, Animation.PlayMode.LOOP);
        AnimationManager turretAnimation = new AnimationManager(enemyTankTurretAtlas, Animation.PlayMode.LOOP);
        AnimationManager shellAnimation = new AnimationManager(shellAtlas, Animation.PlayMode.LOOP);
        AnimationManager explosionAnimation = new AnimationManager(explosionAtlas, Animation.PlayMode.LOOP);

        enemyTanks = new EnemyTank[3];
        enemyTanks[0] = new EnemyTank(movingAnimation, idleAnimation, turretAnimation, shellAnimation, explosionAnimation, stage, playerTank, 2500f, 1000f);
        enemyTanks[1] = new EnemyTank(movingAnimation, idleAnimation, turretAnimation, shellAnimation, explosionAnimation, stage, playerTank, 2000f, 2000f);
        enemyTanks[2] = new EnemyTank(movingAnimation, idleAnimation, turretAnimation, shellAnimation, explosionAnimation, stage, playerTank, 3000f, 3000f);

        for (EnemyTank enemyTank : enemyTanks) {
            stage.addActor(enemyTank);
            activeEnemyTanks.add(enemyTank);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.4f, 0.2f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float deltaTime = Gdx.graphics.getDeltaTime();

        // Update camera position
        float playerX = playerTank.getX();
        float playerY = playerTank.getY();
        float cameraX = MathUtils.lerp(camera.position.x, playerX, CAMERA_LERP_FACTOR);
        float cameraY = MathUtils.lerp(camera.position.y, playerY, CAMERA_LERP_FACTOR);
        camera.position.set(cameraX, cameraY, 0);
        camera.zoom = 0.300f;
        camera.update();

        if (isGameOver) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.draw(backgroundAtlas.findRegion("world"), 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            batch.end();
            gameOverScreen.render();
            return;
        }

        timeSinceLastSpawn += deltaTime;
        if (timeSinceLastSpawn >= SPAWN_INTERVAL && activeEnemyTanks.size < MAX_ENEMY_TANKS) {
            spawnEnemyTank();
            timeSinceLastSpawn = 0;
        }

        stage.act(deltaTime);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(backgroundAtlas.findRegion("world"), 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        stage.draw();

        playerHealthUI.render();
        playerScoreUI.render();
    }

    private void spawnEnemyTank() {
        float cameraLeft = camera.position.x - (camera.viewportWidth / 2);
        float cameraRight = camera.position.x + (camera.viewportWidth / 2);
        float cameraBottom = camera.position.y - (camera.viewportHeight / 2);
        float cameraTop = camera.position.y + (camera.viewportHeight / 2);

        float spawnX, spawnY;
        do {
            spawnX = MathUtils.random(0, WORLD_WIDTH);
            spawnY = MathUtils.random(0, WORLD_HEIGHT);
        } while (spawnX > cameraLeft && spawnX < cameraRight && spawnY > cameraBottom && spawnY < cameraTop);

        TextureAtlas enemyTankMovingAtlas = new TextureAtlas(Gdx.files.internal("atlas/enemy_tank_moving.atlas"));
        TextureAtlas enemyTankIdleAtlas = new TextureAtlas(Gdx.files.internal("atlas/enemy_tank_idle.atlas"));
        TextureAtlas enemyTankTurretAtlas = new TextureAtlas(Gdx.files.internal("atlas/enemy_tank_turret.atlas"));
        TextureAtlas shellAtlas = new TextureAtlas(Gdx.files.internal("atlas/tank_shell.atlas"));
        TextureAtlas explosionAtlas = new TextureAtlas(Gdx.files.internal("atlas/explosion.atlas"));

        AnimationManager movingAnimation = new AnimationManager(enemyTankMovingAtlas, Animation.PlayMode.LOOP);
        AnimationManager idleAnimation = new AnimationManager(enemyTankIdleAtlas, Animation.PlayMode.LOOP);
        AnimationManager turretAnimation = new AnimationManager(enemyTankTurretAtlas, Animation.PlayMode.LOOP);
        AnimationManager shellAnimation = new AnimationManager(shellAtlas, Animation.PlayMode.LOOP);
        AnimationManager explosionAnimation = new AnimationManager(explosionAtlas, Animation.PlayMode.LOOP);

        EnemyTank newEnemyTank = new EnemyTank(movingAnimation, idleAnimation, turretAnimation, shellAnimation, explosionAnimation, stage, playerTank, spawnX, spawnY);
        stage.addActor(newEnemyTank);
        activeEnemyTanks.add(newEnemyTank);
    }

    @Override
    public void resize(final int width, final int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
        if (backgroundAtlas != null) backgroundAtlas.dispose();
        if (playerHealthUI != null) playerHealthUI.dispose();
        if (playerScoreUI != null) playerScoreUI.dispose();
        if (gameOverScreen != null) gameOverScreen.dispose();
    }
}
