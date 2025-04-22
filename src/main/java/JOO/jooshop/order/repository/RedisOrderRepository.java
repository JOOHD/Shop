package JOO.jooshop.order.repository;

import JOO.jooshop.order.entity.TemporaryOrderRedis;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisOrderRepository extends CrudRepository<TemporaryOrderRedis, String> {
}
