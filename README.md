# GimmeMyStickers
GimmeMyStickers, or GMS for short, is a Telegram bot that allows you to extract files from stickers. It is currently in heavy develompent. **It does not work at all.**

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




