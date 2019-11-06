package redis.project.queque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author joy
 * @time 2019/10/18 16:32
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public class QueueDTO {

    private String ref;
    private String method;
    private Object args;
}
