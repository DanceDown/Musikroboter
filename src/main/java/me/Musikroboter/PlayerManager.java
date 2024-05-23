package me.Musikroboter;

import dev.arbjerg.lavalink.client.AbstractAudioLoadResultHandler;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerManager {

    private static PlayerManager INSTANCE;
    private final Map<Long, TrackHandler> trackHandlers;

    private PlayerManager() {
        this.trackHandlers = new HashMap<>();
    }

    public TrackHandler getTrackHandler(Guild g) {
        return this.trackHandlers.computeIfAbsent(g.getIdLong(), (id) -> new TrackHandler(g));
    }

    public void load(TextChannel channel, IReplyCallback hook, String identifier) {

        final boolean isURL = isURL(identifier);
        final String url = (!isURL ? "ytsearch: " : "") + identifier;
        final Guild g = channel.getGuild();

        final TrackHandler handler = this.getTrackHandler(g);

        Link link = Musikroboter.getClient().getOrCreateLink(g.getIdLong());

        link.loadItem(url).subscribe(new AbstractAudioLoadResultHandler() {

            @Override
            public void onSearchResultLoaded(@NotNull SearchResult searchResult) {
                List<Track> tracks = searchResult.getTracks();
                if(tracks.isEmpty()) {
                    hook.replyEmbeds(new EmbedBuilder()
                            .setTitle("Couldn't find track!")
                            .setColor(Color.RED)
                            .setDescription(String.join("","Couldn't find `", url, "`!")).build()).setEphemeral(true).queue();
                    return;
                }
                addTrack(tracks.get(0));
            }

            @Override
            public void onPlaylistLoaded(@NotNull PlaylistLoaded playlistLoaded) {
                final List<Track> list = playlistLoaded.getTracks();
                if(!list.isEmpty()) {
                    if(isURL) {
                        hook.replyEmbeds(new EmbedBuilder()
                                        .setTitle("Adding playlist...")
                                        .setColor(Color.GREEN)
                                        .setDescription(String.join("", "You added the playlist `", playlistLoaded.getInfo().getName(), "`")).build())
                                .setEphemeral(true).complete();
                        for(Track track : list) handler.queue(channel, track);
                    } else {
                        addTrack(list.get(0));
                    }
                }
            }

            @Override
            public void ontrackLoaded(@NotNull TrackLoaded trackLoaded) {
                addTrack(trackLoaded.getTrack());
            }

            @Override
            public void loadFailed(@NotNull LoadFailed loadFailed) {
                System.err.println(loadFailed.getException().getMessage());
                hook.replyEmbeds(new EmbedBuilder()
                        .setTitle("Couldn't be loaded!")
                        .setColor(Color.RED)
                        .setDescription(String.join("","Couldn't load `", url, "`!")).build()).setEphemeral(true).queue();
            }

            @Override
            public void noMatches() {
                hook.replyEmbeds(new EmbedBuilder()
                        .setTitle("Nothing found")
                        .setColor(Color.RED)
                        .setDescription(String.join("", "Couldn't find `", url, "`!")).build()).setEphemeral(true).queue();

            }

            private void addTrack(Track track) {
                if(handler.queue(channel, track)) {
                    hook.replyEmbeds(new EmbedBuilder()
                            .setTitle("Track added!")
                            .setColor(Color.GREEN)
                            .setDescription(String.join("", "You added `", track.getInfo().getTitle(), "` by `",
                                    track.getInfo().getAuthor(), "`")).build()).setEphemeral(true).queue();
                } else {
                    hook.replyEmbeds(new EmbedBuilder()
                            .setTitle("Not adding track")
                            .setColor(Color.RED)
                            .setDescription(String.join("", "Couldn't add `", track.getInfo().getTitle(), "` by `",
                                    track.getInfo().getAuthor(), "`, probably because the list is full")).build()).setEphemeral(true).queue();
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

    public Map<Long, TrackHandler> getTrackHandlers() {
        return Collections.unmodifiableMap(trackHandlers);
    }
}
