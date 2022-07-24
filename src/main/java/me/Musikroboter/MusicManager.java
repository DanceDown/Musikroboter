package me.Musikroboter;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class MusicManager {

    public final AudioPlayer player;
    public final TrackHandler handler;
    public final EqualizerFactory eq = new EqualizerFactory();
    private final AudioPlayerSendHandler sendHandler;

    int bassboost = 0;

    private static final float[] BASS_BOOST = {
            0.2f,
            0.15f,
            0.1f,
            0.05f,
            0.0f,
            -0.05f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f,
            -0.1f
    };

    public MusicManager(AudioPlayerManager manager) {

        this.player = manager.createPlayer();
        this.handler = new TrackHandler(this.player);
        this.player.addListener(this.handler);
        this.player.setFilterFactory(eq);
        this.player.setFrameBufferDuration(1024);
        this.sendHandler = new AudioPlayerSendHandler(this.player);

    }

    public AudioPlayerSendHandler getSendHandler() {
        return this.sendHandler;
    }

    public int bassboost(int percentage) {

        final int temp = bassboost;
        bassboost = percentage;
        final float multiplier = percentage / 100.00f;

        for (int i = 0; i < BASS_BOOST.length; i++)
        {
            eq.setGain(i, BASS_BOOST[i] * multiplier);
        }

        return temp;

    }

}
