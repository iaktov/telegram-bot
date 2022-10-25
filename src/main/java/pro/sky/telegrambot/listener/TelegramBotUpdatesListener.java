package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.Model.NotificationTask;
import pro.sky.telegrambot.Repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;


@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final NotificationTaskRepository notificationTaskRepository;

    static private final String NOTICE_PATTERN = "([0-9.:\\s]{16})(\\s)([\\W+|\\w+]+)";
    static private final Pattern pattern = Pattern.compile(NOTICE_PATTERN);


    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }


    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    //Отправка уведомлений из БД по расписанию, с проверкой каждую минуту
    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotification() {
        Collection<NotificationTask> sentNotifications = notificationTaskRepository
                .findNotificationTasksByDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        sentNotifications.forEach(notificationTask -> {
            long chatId = notificationTask.getChatId();
            String message = notificationTask.getNotificationText();
            if (!message.isEmpty()) {
                SendResponse sendNotification = telegramBot.execute(new SendMessage(chatId,
                        "новое уведомление - " + message));
                logger.info("notification - {}, chatId - {}", message, chatId);
                extracted(sendNotification);
            }
        });
    }

    @Override
    public int process(List<Update> updates) {
        try {
            updates.forEach(update -> {
                long chatId = update.message().chat().id();
                String text = update.message().text();
                logger.info("Processing update: {}", update);
                if (Objects.equals(text, "/start")) {
                    SendResponse response = telegramBot.execute(new SendMessage(chatId,
                            "Привет, я маленькое творение Игоря, могу сохранить твои уведомления" + "\n" +
                                    "Напиши в формате ДД.ММ.ГГГГ ЧЧ:ММ и сам текст уведомления, я сохраню"));
                    extracted(response);
                } else {
                    try {
                        createNotification(update);
                        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                                "Твое уведомление сохранено"));
                        extracted(response);
                        logger.info("Data saved");
                    } catch (DataFormatException e) {
                        logger.warn("Data unsaved");
                        SendResponse response = telegramBot.execute(new SendMessage(chatId,
                                "некорректно введено уведомление, попробуй повторно"));
                        extracted(response);
                    }
                }

            });
        } finally {
            {
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }
        }
    }

    //проверка, что ответ ушел в бот
    private void extracted(SendResponse response) {
        if (!response.isOk()) {
            logger.warn("Response error code is: {}", response.errorCode());
        } else {
            logger.info("Response is: {}", response.isOk());
        }
    }


    //Распознавание данных, введенных пользователем по паттерну
    public List<String> createTextAndDateForNotice(String text) throws DataFormatException {
        Matcher matcher = pattern.matcher(text);
        if (matcher.matches()) {
            String date = matcher.group(1);
            String notification = matcher.group(3);
            return List.of(date, notification);
        } else {
            logger.warn("Incorrect format data");
            throw new DataFormatException("incorrect format data");
        }
    }

    //создание в БД уведомления
    public void createNotification(Update update) throws DataFormatException {
        String message = update.message().text();
        long chatId = update.message().chat().id();
        List<String> textAndDate = new ArrayList<>(createTextAndDateForNotice(message));
        String date = textAndDate.get(0);
        String text = textAndDate.get(1);
        LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        NotificationTask notificationTask = new NotificationTask(chatId, text, localDateTime);
        notificationTaskRepository.save(notificationTask);
        logger.info("Notification save {}", notificationTask);
    }


}
