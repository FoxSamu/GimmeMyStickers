# GimmeMyStickers

GimmeMyStickers, or GMS for short, is a Telegram bot that allows you to convert stickers to files, which you can then download. It is currently in
heavy develompent.

## WIP
Current development status:

- You can send it a sticker and it will convert this sticker to the desired format.
    - Animated stickers are supported but the tools to convert/render these are currently lacking.
    - It will unzip `.tgs` stickers to `.json` files and send those, so they can be directly given to any Lottie animation renderers.
    - It will directly upload `.webm` stickers without conversion.
- The desired image format can be set with commands.
- User preferences are persisted under a hash of the user ID, so that the preferences cannot be traced back to the user.
- When you type `stop` in the standard input of the bot, it will stop the bot.

I consider it nearly done for an early beta release.

## Usage
To build and run the bot from source:
1. Make sure you have a bot token. You can get one from [@BotFather](https://t.me/BotFather).
2. Copy `.env.example` to `.env`.
3. In `.env` (**NOT** in `.env.example`), set `GMS_TOKEN` to your bot token.
4. Navigate a terminal to the project directory.
5. On Linux or Mac, make sure that `gradlew` has execution permissions with `chmod +x gradlew`.
6. Run `./gradlew run`.

**Make sure to never leak your bot token! Always put it in `.env` and never in `.env.example`: `.env` is ignored by Git so this way you won't accidentally commit and leak your bot token. If managed to leak it anyway, revoke your bot token to get a new one from [@BotFather](https://t.me/BotFather)!** Additionally, Gradle will read `.env` and not `.env.example`, so if your token is not in `.env` or `.env` doesn't exist, your bot won't run.

## Technical Details
I built my own Kotlin wrapper around the Telegram Bot API. I found that existing wrappers for Java and Kotlin are rather unintuitive to use, so I chose to build my own wrapper with the features I need. I'm considering extracting it into its own library and expanding it to cover the entire Bot API, but the Bot API is huge so this may take a while. Or it may never happen, don't take my word for it. :P

## License

Copyright &copy; 2025 Samū<br>
All rights reserved

Will add a proper open source license later.


