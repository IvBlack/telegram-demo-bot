# telegram-demo-bot
Based on Spring Boot java telegram bot.


# for deploying on Linux server
1. mvn clean install
2. move .jar to '/opt/bots/tg'
3. write custom service into '/etc/systemd/system' as described into resources/linux_service.txt
4. launch service via systemctl, write '/start' to bot in TG
5. and YAH!
