package org.virep.jdabot.commands.music;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.managers.AudioManager;
import org.virep.jdabot.lavaplayer.AudioLoadHandler;
import org.virep.jdabot.lavaplayer.AudioManagerController;
import org.virep.jdabot.lavaplayer.GuildAudioManager;
import org.virep.jdabot.slashcommandhandler.SlashCommand;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class PlayCommand extends SlashCommand {

    public PlayCommand() {
        super("play",
                "Play music on your voice channel!",
                false,
                new SubcommandData[] {
                        new SubcommandData("youtube", "Play YouTube songs!").addOption(OptionType.STRING, "url", "YouTube Video or Playlist URL").addOption(OptionType.STRING, "search", "Search string"),
                        new SubcommandData("soundcloud", "Play SoundCloud songs!").addOption(OptionType.STRING, "url", "SoundCloud Track or Playlist URL").addOption(OptionType.STRING, "search", "Search string"),
                        new SubcommandData("spotify", "Play Spotify songs").addOption(OptionType.STRING, "url", "Spotify Track or Playlist URL").addOption(OptionType.STRING, "search", "Search string"),
                        new SubcommandData("other", "Play URLs that are not from Spotify/YouTube/SoundCloud").addOption(OptionType.STRING, "url", "URL string", true),
                        new SubcommandData("file", "Play the audio files you attach !").addOption(OptionType.ATTACHMENT, "file", "Audio File", true)
                }
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        GuildAudioManager manager = AudioManagerController.getGuildAudioManager(event.getGuild());

        Guild guild = event.getGuild();

        assert guild != null;
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();

        assert memberVoiceState != null;
        assert selfVoiceState != null;

        OptionMapping urlOption = event.getOption("url");
        OptionMapping searchOption = event.getOption("search");
        OptionMapping fileOption = event.getOption("file");

        String result = "";

        if (urlOption == null && searchOption == null && fileOption == null) {
            event.reply("\u274C - You must select at least an option.").setEphemeral(true).queue();
            return;
        }

        if (urlOption != null && searchOption != null) {
            event.reply("\u274C - You must select only one option. (either `search` or `url`).").setEphemeral(true).queue();
            return;
        }

        if (urlOption != null) {
            try {
                new URL(urlOption.getAsString());
                result = urlOption.getAsString();
            } catch (MalformedURLException e) {
                event.reply("\u274C - You must specify a valid URL.").setEphemeral(true).queue();
                return;
            }
        }

        if (searchOption != null) {
            assert event.getSubcommandName() != null;
            switch (event.getSubcommandName()) {
                case "youtube" -> result = "ytsearch:" + searchOption.getAsString();
                case "soundcloud" -> result = "scsearch:" + searchOption.getAsString();
                case "spotify" -> result = "spsearch:" + searchOption.getAsString();
            }
        }

        if (fileOption != null) {
            Attachment attachment = fileOption.getAsAttachment();

            result = attachment.getProxyUrl();
        }

        if (memberVoiceState.getChannel() == null) {
            event.reply("\u274C - You are not in a voice channel!").setEphemeral(true).queue();
            return;
        }

        if (!selfVoiceState.inAudioChannel()) {
            manager.openConnection((VoiceChannel) memberVoiceState.getChannel());
        } else if (Objects.requireNonNull(selfVoiceState.getChannel()).getIdLong() != memberVoiceState.getChannel().getIdLong()) {
            event.reply("\u274C - You are not in the same channel as me!").setEphemeral(true).queue();
            return;
        }

        AudioManager audioManager = event.getGuild().getAudioManager();
        audioManager.setSelfDeafened(true);

        AudioLoadHandler.loadAndPlay(manager, result, event);
    }
}
