# GimmeMyStickers

GimmeMyStickers, or GMS for short, is a Telegram bot that allows you to convert stickers to files, which you can then download. It is currently in
beta.

You can access the bot at [@GimmeMyStickersBot](https://t.me/GimmeMyStickersBot). To run your own instance, see below.

## Functionality
- You can send it a sticker and it will convert this sticker to the desired format.
    - Animated stickers are supported but the tools to convert/render these are currently lacking.
    - It will unzip `.tgs` stickers to `.json` files and send those, so they can be directly given to any Lottie animation renderers.
    - It will directly upload `.webm` stickers without conversion.
- The desired image format can be set with commands.
- User preferences are persisted under a hash of the user ID, so that the preferences cannot be traced back to the user.

## Running

Before running, make sure you have a bot token. You can get one from [@BotFather](https://t.me/BotFather). Copy `.env.example` to `.env`. In `.env` (*
*NOT** in `.env.example`), set `GMS_TOKEN` to your bot token.

To build and run the bot from source:

1. Navigate a terminal to the project directory.
2. On Linux or Mac, make sure that `gradlew` has execution permissions with `chmod +x gradlew`.
3. Run `./gradlew run`.

To run using Docker Compose:

1. Navigate a terminal to the project directory.
2. Run `docker compose up` (optionally with `--build` to rebuild or `-d` to run detached).

To run manually using Docker:

1. Navigate a terminal to the project directory.
2. Build using `docker build . -t localhost/gms:<version>`.
3. Create a volume using `docker volume create gms`.
4. Run `docker run . --volume gms:/data`.

**Make sure to never leak your bot token! Always put it in `.env` and never in `.env.example`: `.env` is ignored by Git so this way you won't accidentally commit and leak your bot token. If managed to leak it anyway, revoke your bot token to get a new one from [@BotFather](https://t.me/BotFather)!** Additionally, Gradle will read `.env` and not `.env.example`, so if your token is not in `.env` or `.env` doesn't exist, your bot won't run.

## Technical Details
I built my own Kotlin wrapper around the Telegram Bot API. I found that existing wrappers for Java and Kotlin are rather unintuitive to use, so I chose to build my own wrapper with the features I need. I'm considering extracting it into its own library and expanding it to cover the entire Bot API, but the Bot API is huge so this may take a while. Or it may never happen, don't take my word for it. :P

## License

Licensed under the GPL v3 license.

    Copyright &copy; 2025  Olaf W. Nankman

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

