package pro.sky.telegrambot.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.Model.NotificationTask;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface NotificationTaskRepository extends JpaRepository<NotificationTask,Long> {

    //поиск уведомления по заданному времени
    Collection<NotificationTask> findNotificationTasksByDateTime(LocalDateTime localDateTime);



}
