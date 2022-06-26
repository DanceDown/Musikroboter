package me.Musikroboter;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class MusicManager {

    public final AudioPlayer player;
    public final TrackHandler handler;
    private final AudioPlayerSendHandler sendHandler;

    public MusicManager(AudioPlayerManager manager) {

        this.player = manager.createPlayer();
        this.handler = new TrackHandler(this.player);
        this.player.addListener(this.handler);
        this.sendHandler = new AudioPlayerSendHandler(this.player);

    }

    public AudioPlayerSendHandler getSendHandler() {
        return this.sendHandler;
    }

}
