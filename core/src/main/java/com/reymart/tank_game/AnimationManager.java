package com.reymart.tank_game;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class AnimationManager {
    private TextureAtlas atlas;
    private Animation<TextureRegion> animation;
    private float stateTime = 0f;

    public AnimationManager(TextureAtlas atlas, Animation.PlayMode playMode) {
        this.atlas = atlas;
        Array<TextureAtlas.AtlasRegion> regions = atlas.getRegions();
        this.animation = new Animation<>(0.1f, regions);
        this.animation.setPlayMode(playMode);
    }

    public TextureRegion getCurrentFrame(float deltaTime) {
        stateTime += deltaTime;
        return animation.getKeyFrame(stateTime);
    }

    public void resetStateTime() {
        stateTime = 0f;
    }

    public void dispose() {
        if (atlas != null) {
            atlas.dispose();
        }
    }
}
