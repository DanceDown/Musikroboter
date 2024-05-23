package me.Musikroboter;

import dev.arbjerg.lavalink.client.LavalinkNode;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.event.TrackEndEvent;
import dev.arbjerg.lavalink.client.player.FilterBuilder;
import dev.arbjerg.lavalink.client.player.LavalinkPlayer;
import dev.arbjerg.lavalink.client.player.Track;

import dev.arbjerg.lavalink.protocol.v4.Timescale;
import dev.arbjerg.lavalink.protocol.v4.Karaoke;
import dev.arbjerg.lavalink.protocol.v4.LowPass;
import dev.arbjerg.lavalink.protocol.v4.Rotation;
import dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class TrackHandler {

    private final Guild guild;
    private Track currentTrack;
    private LinkedBlockingQueue<Track> queue;
    private BlockingQueue<Track> loopedQueue;
    private Map.Entry<Boolean, Boolean> looped = Map.entry(true, false);
    private final InfoCard infocard;
    private ScheduledExecutorService pauseService;

    private LowPass lowpass;
    private Karaoke karaoke;
    private Rotation panning;
    private Timescale timescale;

    public TrackHandler(Guild guild) {

        LavalinkNode node = Musikroboter.getClient().getNodes().get(0);

        node.on(TrackEndEvent.class).subscribe(this::onTrackEnd);
        this.queue = new LinkedBlockingQueue<>();
        this.loopedQueue = new LinkedBlockingQueue<>();
        this.infocard = new InfoCard();
        this.guild = guild;

    }

    public synchronized boolean queue(TextChannel infoCardChannel, Track track) {

        boolean value = true;

        if(track == null) return false;
        loopedQueue.add(track);

        if(!startTrack(track, false)) {
            value = queue.offer(track);
            if(queue.size() == 1)
                updateInfoCard();
        } else {
            infocard.setChannel(infoCardChannel);
            updateInfoCard();
        }

        return value;

    }

    public boolean jump(int secs) {

        LavalinkPlayer player;
        if((player = getPlayer()) == null) return false;
        Track track = player.getTrack();
        if(track == null) return false;
        long delta = player.getPosition() + secs * 1000L;
        if(delta >= track.getInfo().getLength() && delta <= 0) {
            return false;
        } else {
            player.setPosition(delta).subscribe();
            return true;
        }

    }

    public List<Track> getQueue() {
        List<Track> list = new ArrayList<>(queue.stream().toList());
        list.add(0, currentTrack);
        return list;
    }

    public boolean skip(int num) {

        if(num < 1) return false;
        if(num > queue.size() && loop()) return false;

        for(int i = 1; num > i; i++) {
            queue.poll();
            if(queue.isEmpty()) loop();
        }

        return next();

    }

    public void shuffle() {

        List<Track> temp = new ArrayList<>(queue.stream().toList());
        Collections.shuffle(temp);
        queue = new LinkedBlockingQueue<>(temp);


        temp = new ArrayList<>(loopedQueue.stream().toList());
        Collections.shuffle(temp);
        loopedQueue = new LinkedBlockingQueue<>(temp);

        updateInfoCard();

    }

    public void setLooped(boolean looped, boolean single) {

        this.looped = Map.entry(looped, single);
        updateInfoCard();

    }

    public void stop() {

        LavalinkPlayer player;
        if((player = getPlayer()) == null) return;

        queue.clear();
        loopedQueue.clear();
        player.stopTrack().subscribe();
        currentTrack = null;
        infocard.destroy();
        if(player.getPaused()) player.setPaused(false);

    }

    public boolean pause() {

        LavalinkPlayer player;
        if((player = getPlayer()) == null) return getPlayer().getPaused();
        if(pauseService != null) {
            if(!pauseService.isShutdown()) pauseService.shutdown();
            pauseService = null;
        }

        boolean paused;
        player.setPaused(paused = !player.getPaused()).subscribe();
        Musikroboter.getJda().getPresence().setPresence(paused ? OnlineStatus.IDLE : OnlineStatus.DO_NOT_DISTURB, Activity.playing(paused ? "break" : currentTrack.getInfo().getTitle()));
        infocard.setPaused(paused);
        if(!paused) updateInfoCard();
        return paused;

    }

    public boolean pause(int duration, TimeUnit time) {

        LavalinkPlayer player;
        if((player = getPlayer()) == null) return false;

        boolean setPaused = !player.getPaused();
        Musikroboter.getJda().getPresence().setPresence(setPaused ? OnlineStatus.DO_NOT_DISTURB : OnlineStatus.IDLE,
                Activity.playing(setPaused ? currentTrack.getInfo().getTitle() : "break" + " for " + duration + " " + time.name()));
        pauseService = Executors.newSingleThreadScheduledExecutor();
        pauseService.schedule((Runnable) this::pause, duration, time);
        return setPaused;

    }

    public void volume(int percent) {
        LavalinkPlayer player;
        if((player = getPlayer()) == null) return;
        player.setVolume(percent).subscribe();
    }

    public Map.Entry<Boolean, Boolean> isLooped() {
        return looped;
    }

    // returns false if no tracks to play and looping wasn't successful (e.g. due to looped not being true)
    public boolean next() {

        if(!(looped.getKey() && looped.getValue())) {
            if (queue.isEmpty() && loop()) return false;
            if (looped.getKey() && !looped.getValue() || !looped.getKey() || currentTrack == null)
                currentTrack = queue.poll();
        }
        return startTrack(currentTrack, true);

    }

    // Returns false, if successfully looped
    private boolean loop() {
        if(looped.getKey() && !looped.getValue() && !loopedQueue.isEmpty()) {
            queue = new LinkedBlockingQueue<>(loopedQueue);
            return false;
        }
        return true;
    }

    private volatile int failedTimes = 0;
    public synchronized void onTrackEnd(TrackEndEvent ev) {

        if(ev.getEndReason().getMayStartNext()) {
            if(ev.getEndReason().equals(AudioTrackEndReason.LOAD_FAILED)) {
                System.err.println("FAILED LOADING TRACK: " + currentTrack.getInfo().getUri());
                if(failedTimes < 5) failedTimes++;
                else resetBot();
            }
            if(!next())
                resetBot();

        } else if(ev.getEndReason().equals(AudioTrackEndReason.FINISHED))
            resetBot();

    }

    public void resetBot() {
        infocard.destroy();
        Musikroboter.getJda().getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.listening("/help"));
        failedTimes = 0;
    }

    private synchronized void updateInfoCard() {
        infocard.updateMessage(currentTrack,
                looped.getKey() && looped.getValue() ?
                        currentTrack.getInfo().getTitle()
                        : (queue.peek() == null ? (looped.getKey() && !loopedQueue.isEmpty() ? loopedQueue.peek().getInfo().getTitle() : "None")
                        : queue.peek().getInfo().getTitle()), this);
    }

    public InfoCard getInfocard() {
        return infocard;
    }

    private boolean startTrack(Track track, boolean override) {
        LavalinkPlayer player;
        if((player = getPlayer()) == null)
            return false;
        if(currentTrack != null && !override && player.getTrack() != null)
            return false;
        if(track == null) {
            stop();
            return false;
        }
        currentTrack = track;
        player.setTrack(track.makeClone()).subscribe();
        Musikroboter.getJda().getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.playing(currentTrack.getInfo().getTitle()));
        updateInfoCard();
        return true;
    }

    public LavalinkPlayer getPlayer() {
        Link link = Musikroboter.getClient().getLinkIfCached(guild.getIdLong());
        if(link == null) return null;
        return link.getCachedPlayer();
    }

    public void bassboost(int percent) {
        if(percent <= 0) lowpass = null;
        else lowpass = new LowPass(percent);
        setFilters();
    }

    public void pan(double speed) {
        if(speed < 0.1) panning = null;
        else panning = new Rotation(speed);
        setFilters();
    }

    public void karaoke(float level, float mono, float band, float width) {
        if(level <= 0 || mono < 0 || band < 0 || width < 0) karaoke = null;
        else karaoke = new Karaoke(level, mono, band, width);
        setFilters();
    }

    public void timescale(double speed, double pitch, double rate) {
        if(speed < 0.1 || pitch < 0.1 || rate < 0.1 || (speed == 1 && pitch == 1 && rate == 1)) timescale = null;
        else timescale = new Timescale(speed, pitch, rate);
        setFilters();
    }

    private void setFilters() {
        FilterBuilder builder = new FilterBuilder();
        if(lowpass != null)     builder.setLowPass(lowpass);
        if(karaoke != null)     builder.setKaraoke(karaoke);
        if(timescale != null)   builder.setTimescale(timescale);
        if(panning != null)     builder.setRotation(panning);
        getPlayer().setFilters( builder.build()).subscribe();
        updateInfoCard();
    }

    public boolean getKaraoke() {
        return karaoke != null;
    }

    public boolean getSpeed() {
        return timescale != null;
    }

    public boolean getPanning() {
        return panning != null;
    }

    public boolean getBass() {
        return lowpass != null;
    }
}
