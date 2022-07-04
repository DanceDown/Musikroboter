package me.Musikroboter;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class TrackHandler extends AudioEventAdapter {

    private final AudioPlayer player;
    private AudioTrack currentTrack;
    private BlockingQueue<AudioTrack> queue;
    private BlockingQueue<AudioTrack> loopedQueue;
    private ScheduledExecutorService pauseService;
    private Map.Entry<Boolean, Boolean> looped = Map.entry(true, false);

    public TrackHandler(AudioPlayer player) {

        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        this.loopedQueue = new LinkedBlockingQueue<>();

    }

    public boolean queue(AudioTrack track) {

        if(!player.startTrack(track.makeClone(), true)) {
            loopedQueue.add(track);
            return queue.offer(track);
        } else {
            loopedQueue.add(track);
            currentTrack = track;
            Musikroboter.jda.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.playing(track.getInfo().title));
        }

        return true;

    }

    public boolean jump(int secs) {

        long delta = player.getPlayingTrack().getPosition() + secs * 1000L;
        if(delta >= player.getPlayingTrack().getDuration() && delta <= 0) {
            return false;
        } else {
            player.getPlayingTrack().setPosition(delta);
            return true;
        }

    }

    public List<AudioTrack> getQueue() {
        List<AudioTrack> list = new ArrayList<>(queue.stream().toList());
        list.add(0, currentTrack);
        return list;
    }

    public boolean skip(int num) {

        currentTrack = null;
        if(num < 1 && !next()) return false;
        if(num >= queue.size() && loopNotPossible()) return false;

        for(int i = 1; num > i; i++) {
            queue.poll();
        }

        next();

        return true;

    }

    public void shuffle() {

        List<AudioTrack> temp = new ArrayList<>(queue.stream().toList());
        Collections.shuffle(temp);
        queue = new LinkedBlockingQueue<>(temp);


        temp = new ArrayList<>(loopedQueue.stream().toList());
        Collections.shuffle(temp);
        loopedQueue = new LinkedBlockingQueue<>(temp);

    }

    public void setLooped(boolean looped, boolean single) {

        this.looped = Map.entry(looped, single);

    }

    public void stop() {

        queue.clear();
        loopedQueue.clear();
        player.stopTrack();

    }

    public boolean pause() {

        boolean paused;
        player.setPaused(paused = !player.isPaused());
        Musikroboter.jda.getPresence().setPresence(paused ? OnlineStatus.IDLE : OnlineStatus.DO_NOT_DISTURB, Activity.playing(paused ? "break" : currentTrack.getInfo().title));
        return paused;

    }

    public boolean pause(int duration, TimeUnit time) {

        boolean setPaused = !player.isPaused();
        Musikroboter.jda.getPresence().setPresence(setPaused ? OnlineStatus.DO_NOT_DISTURB : OnlineStatus.IDLE, Activity.playing(setPaused ? currentTrack.getInfo().title : "break" + " for " + duration + " " + time.name()));
        pauseService = Executors.newSingleThreadScheduledExecutor();
        pauseService.schedule((Runnable) this::pause, duration, time);
        return setPaused;

    }

    public void volume(int percent) {
        player.setVolume(percent);
    }

    public Map.Entry<Boolean, Boolean> isLooped() {
        return looped;
    }

    // returns false if no tracks to play and looping wasn't successful (e.g. due to looped not being true)
    public boolean next() {

        if(queue.isEmpty() && loopNotPossible()) return false;

        if(looped.getKey() && !looped.getValue() || !looped.getKey() || currentTrack == null) currentTrack = queue.poll();
        if(currentTrack == null) return false;
        player.playTrack(currentTrack.makeClone());
        return true;

    }

    // Returns false, if successfully looped
    private boolean loopNotPossible() {
        if(looped.getKey() && (looped.getValue() || !loopedQueue.isEmpty())) {
            queue = new LinkedBlockingQueue<>(loopedQueue);
            return false;
        }
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

        if(endReason.mayStartNext) {
            if(next()) {
                Musikroboter.jda.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.playing(currentTrack.getInfo().title));
            } else {
                Musikroboter.jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.listening("/help"));
            }
        }

    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        if(pauseService != null && !pauseService.isShutdown()) pauseService.shutdownNow();
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        if(pauseService != null && !pauseService.isShutdown()) pauseService.shutdownNow();
    }
}
