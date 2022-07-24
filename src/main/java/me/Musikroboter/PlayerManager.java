package me.Musikroboter;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerManager {

    private static PlayerManager INSTANCE;
    private final Map<Long, MusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    public PlayerManager() {

        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(this.playerManager);
        AudioSourceManagers.registerLocalSource(this.playerManager);

        playerManager.getConfiguration().setFilterHotSwapEnabled(true);

    }

    public MusicManager getMusicManager(Guild g) {
        return this.musicManagers.computeIfAbsent(g.getIdLong(), (id) -> {
            final MusicManager manager = new MusicManager(this.playerManager);
            g.getAudioManager().setSendingHandler(manager.getSendHandler());
            return manager;
        });
    }

    public void load(TextChannel channel, IReplyCallback hook, String url) {

        boolean isURL;
        final String link;
        if(!(isURL = isURL(url))) {
            link = String.join(" ", "ytsearch:", url);
        } else link = url;

        final MusicManager manager = this.getMusicManager(channel.getGuild());

        this.playerManager.loadItemOrdered(manager, link, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                addTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

                final List<AudioTrack> list = playlist.getTracks();
                if(!list.isEmpty()) {
                    if(isURL) {
                        for(AudioTrack track : list) manager.handler.queue(channel, track);
                        hook.replyEmbeds(new EmbedBuilder()
                                .setTitle("Adding playlist...")
                                .setColor(Color.GREEN)
                                .setDescription(String.join("","You added the playlist `",playlist.getName(), "`")).build()).setEphemeral(true).queue();
                    } else {
                        addTrack(list.get(0));
                    }
                }

            }

            @Override
            public void noMatches() {

                hook.replyEmbeds(new EmbedBuilder()
                        .setTitle("Nothing found")
                        .setColor(Color.RED)
                        .setDescription(String.join("", "Couldn't find `", link, "`!")).build()).setEphemeral(true).queue();

            }

            @Override
            public void loadFailed(FriendlyException ex) {

                hook.replyEmbeds(new EmbedBuilder()
                        .setTitle("Couldn't be loaded!")
                        .setColor(Color.RED)
                        .setDescription(String.join("","Couldn't load `", url, "`!")).build()).setEphemeral(true).queue();

            }

            private void addTrack(AudioTrack track) {
                if(manager.handler.queue(channel, track)) {
                    hook.replyEmbeds(new EmbedBuilder()
                            .setTitle("Adding track...")
                            .setColor(Color.GREEN)
                            .setDescription(String.join("", "You added `", track.getInfo().title, "` by `",
                                    track.getInfo().author, "`")).build()).setEphemeral(true).queue();
                } else {
                    hook.replyEmbeds(new EmbedBuilder()
                            .setTitle("Not adding track")
                            .setColor(Color.RED)
                            .setDescription(String.join("", "Couldn't add `", track.getInfo().title, "` by `",
                                    track.getInfo().author, "`, probably because the list is full")).build()).setEphemeral(true).queue();
                }
            }
        });

    }

    private boolean isURL(String link) {

        try {
            new URI(link);
            return true;
        } catch(URISyntaxException ex) {
            return false;
        }
    }

    public static PlayerManager getINSTANCE() {
        if(INSTANCE == null) INSTANCE = new PlayerManager();
        return INSTANCE;
    }
}
