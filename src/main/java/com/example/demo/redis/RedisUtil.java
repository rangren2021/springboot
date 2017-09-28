package com.example.demo.redis;

import com.btjf.common.redis.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;

import java.util.*;


/**
 * 内存数据库Redis的辅助类，负责对内存数据库的所有操作
 * @version V1.0
 * @author fengjc
 */
@Component
public class RedisUtil {

	// 数据源
	@Autowired
	private ShardedJedisPool shardedJedisPool;

	/**
	 * 执行器，{@link com.epsoft.eiss.base.redis.util.futurefleet.framework.base.redis.RedisUtil}的辅助类，
	 * 它保证在执行操作之后释放数据源returnResource(jedis)
	 * @version V1.0
	 * @author fengjc
	 * @param <T>
	 */
	abstract class Executor<T> {

		ShardedJedis jedis;
		ShardedJedisPool shardedJedisPool;

		public Executor(ShardedJedisPool shardedJedisPool) {
			this.shardedJedisPool = shardedJedisPool;
			jedis = this.shardedJedisPool.getResource();
		}

		/**
		 * 回调
		 * @return 执行结果
		 */
		abstract T execute();

		/**
		 * 调用{@link #execute()}并返回执行结果
		 * 它保证在执行{@link #execute()}之后释放数据源returnResource(jedis)
		 * @return 执行结果
		 */
		public T getResult() {
			T result = null;
			try {
				result = execute();
			} catch (Throwable e) {
				throw new RuntimeException("Redis execute exception", e);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}
			return result;
		}
	}

	/**
	 * 删除模糊匹配的key
	 * @param likeKey 模糊匹配的key
	 * @return 删除成功的条数
	 */
	public long delKeysLike(final String likeKey) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				Collection<Jedis> jedisC = jedis.getAllShards();
				Iterator<Jedis> iter = jedisC.iterator();
				long count = 0;
				while (iter.hasNext()) {
					Jedis _jedis = iter.next();
					Set<String> keys = _jedis.keys(likeKey + "*");
					count += _jedis.del(keys.toArray(new String[keys.size()]));
				}
				return count;
			}
		}.getResult();
	}

	/**
	 * 删除
	 * @param key 匹配的key
	 * @return 删除成功的条数
	 */
	public Long delKey(final String key) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.del(key);
			}
		}.getResult();
	}

	/**
	 * 删除
	 * @param keys 匹配的key的集合
	 * @return 删除成功的条数
	 */
	public Long delKeys(final String[] keys) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				Collection<Jedis> jedisC = jedis.getAllShards();
				Iterator<Jedis> iter = jedisC.iterator();
				long count = 0;
				while (iter.hasNext()) {
					Jedis _jedis = iter.next();
					count += _jedis.del(keys);
				}
				return count;
			}
		}.getResult();
	}

	/**
	 * 删除指定Key中指定的field
	 * @param key 匹配的key
	 * @param fields 匹配的field
	 * @return
	 */
	public Long hdelFields(final String key, final String... fields){
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.hdel(key, fields);
			}
		}.getResult();
	}

	/**
	 * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
	 * 在 Redis 中，带有生存时间的 key 被称为『可挥发』(volatile)的。
	 * @param key key
	 * @param expire 生命周期，单位为秒
	 * @return 1: 设置成功 0: 已经超时或key不存在
	 */
	public Long expire(final String key, final int expire) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.expire(key, expire);
			}
		}.getResult();
	}

	/**
	 * 一个跨jvm的id生成器，利用了redis原子性操作的特点
	 * @param key id的key
	 * @return 返回生成的Id
	 */
	public long makeId(final String key) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				long id = jedis.incr(key);
				if ((id + 75807) >= Long.MAX_VALUE) {
					// 避免溢出，重置，getSet命令之前允许incr插队，75807就是预留的插队空间
					jedis.getSet(key, "0");
				}
				return id;
			}
		}.getResult();
	}

	/* ======================================Strings====================================== */

	/**
	 * 将字符串值 value 关联到 key 。
	 * 如果 key 已经持有其他值， setString 就覆写旧值，无视类型。
	 * 对于某个原本带有生存时间（TTL）的键来说， 当 setString 成功在这个键上执行时， 这个键原有的 TTL 将被清除。
	 * 时间复杂度：O(1)
	 * @param key key
	 * @param value string value
	 * @return 在设置操作成功完成时，才返回 OK 。
	 */
	public String setString(final String key, final String value) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				return jedis.set(key, value);
			}
		}.getResult();
	}

	/**
	 * 将值 value 关联到 key ，并将 key 的生存时间设为 expire (以秒为单位)。
	 * 如果 key 已经存在， 将覆写旧值。
	 * 类似于以下两个命令:
	 * SET key value
	 * EXPIRE key expire # 设置生存时间
	 * 不同之处是这个方法是一个原子性(atomic)操作，关联值和设置生存时间两个动作会在同一时间内完成，在 Redis 用作缓存时，非常实用。
	 * 时间复杂度：O(1)
	 * @param key key
	 * @param value string value
	 * @param expire 生命周期
	 * @return 设置成功时返回 OK 。当 expire 参数不合法时，返回一个错误。
	 */
	public String setString(final String key, final String value, final int expire) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				return jedis.setex(key, expire, value);
			}
		}.getResult();
	}

	/**
	 * 将 key 的值设为 value ，当且仅当 key 不存在。若给定的 key 已经存在，则 setStringIfNotExists 不做任何动作。
	 * 时间复杂度：O(1)
	 * @param key key
	 * @param value string value
	 * @return 设置成功，返回 1 。设置失败，返回 0 。
	 */
	public Long setStringIfNotExists(final String key, final String value) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.setnx(key, value);
			}
		}.getResult();
	}

	/**
	 * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)。
	 * @param key key
	 * @param value value
	 * @return 返回旧值
	 */
	public String getSet(final String key, final String value) {
		return new Executor<String>(shardedJedisPool) {
			@Override
			String execute() {
				return jedis.getSet(key, value);
			}
		}.getResult();
	}

	/**
	 * redis计数器，讲key 中储存的数字值增一，如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作
	 * @param key key
	 * @return 数字
	 */
	public Long incr(final String key) {
		return new Executor<Long>(shardedJedisPool) {
			@Override
			Long execute() {
				return jedis.incr(key);
			}
		}.getResult();
	}

	/**
	 * 返回 key 所关联的字符串值。如果 key 不存在那么返回特殊值 nil 。
	 * 假如 key 储存的值不是字符串类型，返回一个错误，因为 getString 只能用于处理字符串值。
	 * 时间复杂度: O(1)
	 * @param key key
	 * @return 当 key 不存在时，返回 nil ，否则，返回 key 的值。如果 key 不是字符串类型，那么返回一个错误。
	 */
	public String getString(final String key) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				return jedis.get(key);
			}
		}.getResult();
	}

	/**
	 * 批量的 {@link #setString(String, String)}
	 * @param pairs 键值对数组{数组第一个元素为key，第二个元素为value}
	 * @return 操作状态的集合
	 */
	public List<Object> batchSetString(final List<Pair<String, String>> pairs) {
		return new Executor<List<Object>>(shardedJedisPool) {

			@Override
			List<Object> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();
				for (Pair<String, String> pair : pairs) {
					pipeline.set(pair.getKey(), pair.getValue());
				}
				return pipeline.syncAndReturnAll();
			}
		}.getResult();
	}

	/**
	 * 批量的 {@link #getString(String)}
	 * @param keys key数组
	 * @return value的集合
	 */
	public List<String> batchGetString(final String[] keys) {
		return new Executor<List<String>>(shardedJedisPool) {

			@Override
			List<String> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();
				List<String> result = new ArrayList<String>(keys.length);
				List<Response<String>> responses = new ArrayList<Response<String>>(keys.length);
				for (String key : keys) {
					responses.add(pipeline.get(key));
				}
				pipeline.sync();
				for (Response<String> resp : responses) {
					result.add(resp.get());
				}
				return result;
			}
		}.getResult();
	}

	/* ======================================Hashes====================================== */

	/**
	 * 将哈希表 key 中的域 field 的值设为 value 。
	 * 如果 key 不存在，一个新的哈希表被创建并进行 hashSet 操作。
	 * 如果域 field 已经存在于哈希表中，旧值将被覆盖。
	 * 时间复杂度: O(1)
	 * @param key key
	 * @param field 域
	 * @param value string value
	 * @return 如果 field 是哈希表中的一个新建域，并且值设置成功，返回 1 。如果哈希表中域 field 已经存在且旧值已被新值覆盖，返回 0 。
	 */
	public Long hashSet(final String key, final String field, final String value) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.hset(key, field, value);
			}
		}.getResult();
	}

	/**
	 * 将哈希表 key 中的域 field 的值设为 value 。
	 * 如果 key 不存在，一个新的哈希表被创建并进行 hashSet 操作。
	 * 如果域 field 已经存在于哈希表中，旧值将被覆盖。
	 * @param key key
	 * @param field 域
	 * @param value string value
	 * @param expire 生命周期，单位为秒
	 * @return 如果 field 是哈希表中的一个新建域，并且值设置成功，返回 1 。如果哈希表中域 field 已经存在且旧值已被新值覆盖，返回 0 。
	 */
	public Long hashSet(final String key, final String field, final String value, final int expire) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				Pipeline pipeline = jedis.getShard(key).pipelined();
				Response<Long> result = pipeline.hset(key, field, value);
				pipeline.expire(key, expire);
				pipeline.sync();
				return result.get();
			}
		}.getResult();
	}

	/**
	 * 返回哈希表 key 中给定域 field 的值。
	 * 时间复杂度:O(1)
	 * @param key key
	 * @param field 域
	 * @return 给定域的值。当给定域不存在或是给定 key 不存在时，返回 nil 。
	 */
	public String hashGet(final String key, final String field) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				return jedis.hget(key, field);
			}
		}.getResult();
	}

	/**
	 * 返回哈希表 key 中给定域 field 的值。 如果哈希表 key 存在，同时设置这个 key 的生存时间
	 * @param key key
	 * @param field 域
	 * @param expire 生命周期，单位为秒
	 * @return 给定域的值。当给定域不存在或是给定 key 不存在时，返回 nil 。
	 */
	public String hashGet(final String key, final String field, final int expire) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				Pipeline pipeline = jedis.getShard(key).pipelined();
				Response<String> result = pipeline.hget(key, field);
				pipeline.expire(key, expire);
				pipeline.sync();
				return result.get();
			}
		}.getResult();
	}

	/**
	 * 同时将多个 field-value (域-值)对设置到哈希表 key 中。
	 * 时间复杂度: O(N) (N为fields的数量)
	 * @param key key
	 * @param hash field-value的map
	 * @return 如果命令执行成功，返回 OK 。当 key 不是哈希表(hash)类型时，返回一个错误。
	 */
	public String hashMultipleSet(final String key, final Map<String, String> hash) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				return jedis.hmset(key, hash);
			}
		}.getResult();
	}

	/**
	 * 同时将多个 field-value (域-值)对设置到哈希表 key 中。同时设置这个 key 的生存时间
	 * @param key key
	 * @param hash field-value的map
	 * @param expire 生命周期，单位为秒
	 * @return 如果命令执行成功，返回 OK 。当 key 不是哈希表(hash)类型时，返回一个错误。
	 */
	public String hashMultipleSet(final String key, final Map<String, String> hash, final int expire) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				Pipeline pipeline = jedis.getShard(key).pipelined();
				Response<String> result = pipeline.hmset(key, hash);
				pipeline.expire(key, expire);
				pipeline.sync();
				return result.get();
			}
		}.getResult();
	}

	/**
	 * 返回哈希表 key 中，一个或多个给定域的值。如果给定的域不存在于哈希表，那么返回一个 nil 值。
	 * 时间复杂度: O(N) (N为fields的数量)
	 * @param key key
	 * @param fields field的数组
	 * @return 一个包含多个给定域的关联值的表，表值的排列顺序和给定域参数的请求顺序一样。
	 */
	public List<String> hashMultipleGet(final String key, final String... fields) {
		return new Executor<List<String>>(shardedJedisPool) {

			@Override
			List<String> execute() {
				return jedis.hmget(key, fields);
			}
		}.getResult();
	}

	/**
	 * 返回哈希表 key 中，一个或多个给定域的值。如果给定的域不存在于哈希表，那么返回一个 nil 值。
	 * 同时设置这个 key 的生存时间
	 * @param key key
	 * @param fields field的数组
	 * @param expire 生命周期，单位为秒
	 * @return 一个包含多个给定域的关联值的表，表值的排列顺序和给定域参数的请求顺序一样。
	 */
	public List<String> hashMultipleGet(final String key, final int expire, final String... fields) {
		return new Executor<List<String>>(shardedJedisPool) {

			@Override
			List<String> execute() {
				Pipeline pipeline = jedis.getShard(key).pipelined();
				Response<List<String>> result = pipeline.hmget(key, fields);
				pipeline.expire(key, expire);
				pipeline.sync();
				return result.get();
			}
		}.getResult();
	}

	/**
	 * 批量的{@link #hashMultipleSet(String, Map)}，在管道中执行
	 * @param pairs 多个hash的多个field
	 * @return 操作状态的集合
	 */
	public List<Object> batchHashMultipleSet(final List<Pair<String, Map<String, String>>> pairs) {
		return new Executor<List<Object>>(shardedJedisPool) {

			@Override
			List<Object> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();
				for (Pair<String, Map<String, String>> pair : pairs) {
					pipeline.hmset(pair.getKey(), pair.getValue());
				}
				return pipeline.syncAndReturnAll();
			}
		}.getResult();
	}

	/**
	 * 批量的{@link #hashMultipleSet(String, Map)}，在管道中执行
	 * @param data Map<String, Map<String, String>>格式的数据
	 * @return 操作状态的集合
	 */
	public List<Object> batchHashMultipleSet(final Map<String, Map<String, String>> data) {
		return new Executor<List<Object>>(shardedJedisPool) {

			@Override
			List<Object> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();
				for (Map.Entry<String, Map<String, String>> iter : data.entrySet()) {
					pipeline.hmset(iter.getKey(), iter.getValue());
				}
				return pipeline.syncAndReturnAll();
			}
		}.getResult();
	}

	/**
	 * 批量的{@link #hashMultipleGet(String, String...)}，在管道中执行
	 * @param pairs 多个hash的多个field
	 * @return 执行结果的集合
	 */
	public List<List<String>> batchHashMultipleGet(final List<Pair<String, String[]>> pairs) {
		return new Executor<List<List<String>>>(shardedJedisPool) {

			@Override
			List<List<String>> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();
				List<List<String>> result = new ArrayList<List<String>>(pairs.size());
				List<Response<List<String>>> responses = new ArrayList<Response<List<String>>>(pairs.size());
				for (Pair<String, String[]> pair : pairs) {
					responses.add(pipeline.hmget(pair.getKey(), pair.getValue()));
				}
				pipeline.sync();
				for (Response<List<String>> resp : responses) {
					result.add(resp.get());
				}
				return result;
			}
		}.getResult();

	}

	/**
	 * 返回哈希表 key 中，所有的域和值。在返回值里，紧跟每个域名(field name)之后是域的值(value)，所以返回值的长度是哈希表大小的两倍。
	 * 时间复杂度: O(N)
	 * @param key key
	 * @return 以列表形式返回哈希表的域和域的值。若 key 不存在，返回空列表。
	 */
	public Map<String, String> hashGetAll(final String key) {
		return new Executor<Map<String, String>>(shardedJedisPool) {

			@Override
			Map<String, String> execute() {
				return jedis.hgetAll(key);
			}
		}.getResult();
	}

	/**
	 * 返回哈希表 key 中，所有的域和值。在返回值里，紧跟每个域名(field name)之后是域的值(value)，所以返回值的长度是哈希表大小的两倍。
	 * 同时设置这个 key 的生存时间
	 * @param key key
	 * @param expire 生命周期，单位为秒
	 * @return 以列表形式返回哈希表的域和域的值。若 key 不存在，返回空列表。
	 */
	public Map<String, String> hashGetAll(final String key, final int expire) {
		return new Executor<Map<String, String>>(shardedJedisPool) {

			@Override
			Map<String, String> execute() {
				Pipeline pipeline = jedis.getShard(key).pipelined();
				Response<Map<String, String>> result = pipeline.hgetAll(key);
				pipeline.expire(key, expire);
				pipeline.sync();
				return result.get();
			}
		}.getResult();
	}

	/**
	 * 批量的{@link #hashGetAll(String)}
	 * @param keys key的数组
	 * @return 执行结果的集合
	 */
	public List<Map<String, String>> batchHashGetAll(final String... keys) {
		return new Executor<List<Map<String, String>>>(shardedJedisPool) {

			@Override
			List<Map<String, String>> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();
				List<Map<String, String>> result = new ArrayList<Map<String, String>>(keys.length);
				List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String, String>>>(keys.length);
				for (String key : keys) {
					responses.add(pipeline.hgetAll(key));
				}
				pipeline.sync();
				for (Response<Map<String, String>> resp : responses) {
					result.add(resp.get());
				}
				return result;
			}
		}.getResult();
	}

	/**
	 * 批量的{@link #hashMultipleGet(String, String...)}，与{@link #batchHashGetAll(String...)}不同的是，返回值为Map类型
	 * @param keys key的数组
	 * @return 多个hash的所有filed和value
	 */
	public Map<String, Map<String, String>> batchHashGetAllForMap(final String... keys) {
		return new Executor<Map<String, Map<String, String>>>(shardedJedisPool) {

			@Override
			Map<String, Map<String, String>> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();

				// 设置map容量防止rehash
				int capacity = 1;
				while ((int) (capacity * 0.75) <= keys.length) {
					capacity <<= 1;
				}
				Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>(capacity);
				List<Response<Map<String, String>>> responses = new ArrayList<Response<Map<String, String>>>(keys.length);
				for (String key : keys) {
					responses.add(pipeline.hgetAll(key));
				}
				pipeline.sync();
				for (int i = 0; i < keys.length; ++i) {
					result.put(keys[i], responses.get(i).get());
				}
				return result;
			}
		}.getResult();
	}

	/* ======================================List====================================== */

	/**
	 * 将一个或多个值 value 插入到列表 key 的表尾(最右边)。
	 * @param key key
	 * @param values value的数组
	 * @return 执行 listPushTail 操作后，表的长度
	 */
	public Long listPushEnd(final String key, final String... values) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.rpush(key, values);
			}
		}.getResult();
	}

	/**
	 * 将一个或多个值 value 插入到列表 key 的表头
	 * @param key key
	 * @param value string value
	 * @return 执行 listPushHead 命令后，列表的长度。
	 */
	public Long listPushHead(final String key, final String value) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.lpush(key, value);
			}
		}.getResult();
	}

	/**
	 * 将一个或多个值 value 插入到列表 key 的表尾(最右边)。
	 * @param key key
	 * @param values value的数组
	 * @return 执行 listPushTail 操作后，表的长度
	 */
	public String listPopEnd(final String key) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				return jedis.rpop(key);
			}
		}.getResult();
	}

	/**
	 * 将一个或多个值 value 插入到列表 key 的表头
	 * @param key key
	 * @param value string value
	 * @return 执行 listPushHead 命令后，列表的长度。
	 */
	public String listPopHead(final String key) {
		return new Executor<String>(shardedJedisPool) {

			@Override
			String execute() {
				return jedis.lpop(key);
			}
		}.getResult();
	}

	/**
	 * 将一个或多个值 value 插入到列表 key 的表头, 当列表大于指定长度是就对列表进行修剪(trim)
	 * @param key key
	 * @param value string value
	 * @param size 链表超过这个长度就修剪元素
	 * @return 执行 listPushHeadAndTrim 命令后，列表的长度。
	 */
	public Long listPushHeadAndTrim(final String key, final String value, final long size) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				Pipeline pipeline = jedis.getShard(key).pipelined();
				Response<Long> result = pipeline.lpush(key, value);
				// 修剪列表元素, 如果 size - 1 比 end 下标还要大，Redis将 size 的值设置为 end 。
				pipeline.ltrim(key, 0, size - 1);
				pipeline.sync();
				return result.get();
			}
		}.getResult();
	}

	/**
	 * 批量的{@link #listPushTail(String, String...)}，以锁的方式实现
	 * @param key key
	 * @param values value的数组
	 * @param delOld 如果key存在，是否删除它。true 删除；false: 不删除，只是在行尾追加
	 */
	public void batchListPushTail(final String key, final String[] values, final boolean delOld) {
		new Executor<Object>(shardedJedisPool) {

			@Override
			Object execute() {
				if (delOld) {
					RedisLock lock = new RedisLock(key, shardedJedisPool);
					lock.lock();
					try {
						Pipeline pipeline = jedis.getShard(key).pipelined();
						pipeline.del(key);
						for (String value : values) {
							pipeline.rpush(key, value);
						}
						pipeline.sync();
					} finally {
						lock.unlock();
					}
				} else {
					jedis.rpush(key, values);
				}
				return null;
			}
		}.getResult();
	}

	/**
	 * 同{@link #batchListPushTail(String, String[], boolean)},不同的是利用redis的事务特性来实现
	 * @param key key
	 * @param values value的数组
	 * @return null
	 */
	public Object updateListInTransaction(final String key, final List<String> values) {
		return new Executor<Object>(shardedJedisPool) {

			@Override
			Object execute() {
				Transaction transaction = jedis.getShard(key).multi();
				transaction.del(key);
				for (String value : values) {
					transaction.rpush(key, value);
				}
				transaction.exec();
				return null;
			}
		}.getResult();
	}

	/**
	 * 在key对应list的尾部部添加字符串元素,如果key存在，什么也不做
	 * @param key key
	 * @param values value的数组
	 * @return 执行insertListIfNotExists后，表的长度
	 */
	public Long insertListIfNotExists(final String key, final String[] values) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				RedisLock lock = new RedisLock(key, shardedJedisPool);
				lock.lock();
				try {
					if (!jedis.exists(key)) {
						return jedis.rpush(key, values);
					}
				} finally {
					lock.unlock();
				}
				return 0L;
			}
		}.getResult();
	}

	/**
	 * 返回list所有元素，下标从0开始，负值表示从后面计算，-1表示倒数第一个元素，key不存在返回空列表
	 * @param key key
	 * @return list所有元素
	 */
	public List<String> listGetAll(final String key) {
		return new Executor<List<String>>(shardedJedisPool) {

			@Override
			List<String> execute() {
				return jedis.lrange(key, 0, -1);
			}
		}.getResult();
	}

	/**
	 * 返回指定区间内的元素，下标从0开始，负值表示从后面计算，-1表示倒数第一个元素，key不存在返回空列表
	 * @param key key
	 * @param beginIndex 下标开始索引（包含）
	 * @param endIndex 下标结束索引（不包含）
	 * @return 指定区间内的元素
	 */
	public List<String> listRange(final String key, final long beginIndex, final long endIndex) {
		return new Executor<List<String>>(shardedJedisPool) {

			@Override
			List<String> execute() {
				return jedis.lrange(key, beginIndex, endIndex - 1);
			}
		}.getResult();
	}

	/**
	 * 一次获得多个链表的数据
	 * @param keys key的数组
	 * @return 执行结果
	 */
	public Map<String, List<String>> batchGetAllList(final List<String> keys) {
		return new Executor<Map<String, List<String>>>(shardedJedisPool) {

			@Override
			Map<String, List<String>> execute() {
				ShardedJedisPipeline pipeline = jedis.pipelined();
				Map<String, List<String>> result = new HashMap<String, List<String>>();
				List<Response<List<String>>> responses = new ArrayList<Response<List<String>>>(keys.size());
				for (String key : keys) {
					responses.add(pipeline.lrange(key, 0, -1));
				}
				pipeline.sync();
				for (int i = 0; i < keys.size(); ++i) {
					result.put(keys.get(i), responses.get(i).get());
				}
				return result;
			}
		}.getResult();
	}


	/**
	 * 根据参数 count 的值，移除列表中与参数 value 相等的元素。
	 * http://doc.redisfans.com/list/lrem.html
	 * @param key   key
	 * @param count 移除所有与value值相同的元素。0表示移除所有相同的值
	 * @param value value值
	 * @return 执行结果
	 */
	public Long listLrem(final String key, final long count, final String value) {
		return new Executor<Long>(shardedJedisPool) {
			@Override
			Long execute() {
				return jedis.lrem(key, count, value);
			}
		}.getResult();
	}



	/* ======================================Pub/Sub====================================== */

	/**
	 * 将信息 message 发送到指定的频道 channel。
	 * 时间复杂度：O(N+M)，其中 N 是频道 channel 的订阅者数量，而 M 则是使用模式订阅(subscribed patterns)的客户端的数量。
	 * @param channel 频道
	 * @param message 信息
	 * @return 接收到信息 message 的订阅者数量。
	 */
	public Long publish(final String channel, final String message) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				Jedis _jedis = jedis.getShard(channel);
				return _jedis.publish(channel, message);
			}

		}.getResult();
	}

	/**
	 * 订阅给定的一个频道的信息。
	 * @param jedisPubSub 监听器
	 * @param channel 频道
	 */
	public void subscribe(final JedisPubSub jedisPubSub, final String channel) {
		new Executor<Object>(shardedJedisPool) {

			@Override
			Object execute() {
				Jedis _jedis = jedis.getShard(channel);
				// 注意subscribe是一个阻塞操作，因为当前线程要轮询Redis的响应然后调用subscribe
				_jedis.subscribe(jedisPubSub, channel);
				return null;
			}
		}.getResult();
	}

	/**
	 * 取消订阅
	 * @param jedisPubSub 监听器
	 */
	public void unSubscribe(final JedisPubSub jedisPubSub) {
		jedisPubSub.unsubscribe();
	}

	/* ======================================Sorted set================================= */

	/**
	 * 将一个 member 元素及其 score 值加入到有序集 key 当中。
	 * @param key key
	 * @param score score 值可以是整数值或双精度浮点数。
	 * @param member 有序集的成员
	 * @return 被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员。
	 */
	public Long addWithSortedSet(final String key, final double score, final String member) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.zadd(key, score, member);
			}
		}.getResult();
	}

//	/**
//	 * 将多个 member 元素及其 score 值加入到有序集 key 当中。
//	 * @param key key
//	 * @param scoreMembers score、member的pair
//	 * @return 被成功添加的新成员的数量，不包括那些被更新的、已经存在的成员。
//	 */
//	public Long addWithSortedSet(final String key, final Map<Double, String> scoreMembers) {
//		return new Executor<Long>(shardedJedisPool) {
//
//			@Override
//			Long execute() {
//				return jedis.add(key, scoreMembers);
//			}
//		}.getResult();
//	}

	/**
	 * 返回有序集 key 中， score 值介于 max 和 min 之间(默认包括等于 max 或 min )的所有的成员。
	 * 有序集成员按 score 值递减(从大到小)的次序排列。
	 * @param key key
	 * @param max score最大值
	 * @param min score最小值
	 * @return 指定区间内，带有 score 值(可选)的有序集成员的列表
	 */
	public Set<String> revrangeByScoreWithSortedSet(final String key, final double max, final double min) {
		return new Executor<Set<String>>(shardedJedisPool) {

			@Override
			Set<String> execute() {
				return jedis.zrevrangeByScore(key, max, min);
			}
		}.getResult();
	}


	/**
	 * Redis Sadd 命令将一个或多个成员元素加入到集合中，已经存在于集合的成员元素将被忽略。
	 假如集合 key 不存在，则创建一个只包含添加的元素作成员的集合。
	 当集合 key 不是集合类型时，返回一个错误。
	 注意：在Redis2.4版本以前， SADD 只接受单个成员值。
	 * @param key
	 * @param value
	 * @return
	 */
	public Integer sadd(String key, String value){
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.sadd(key, value);
			}
		}.getResult().intValue();
	}

	/**
	 *
	 * 获取哈希表中字段的数量
	 * @param key
	 * @return
	 */
	public Long hlen(String key) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.hlen(key);
			}
		}.getResult();
	}

	/**
	 *
	 * 获取list长度
	 * @param key
	 * @return
	 */
	public Long llen(String key) {
		return new Executor<Long>(shardedJedisPool) {
			@Override
			Long execute() {
				return jedis.llen(key);
			}
		}.getResult();
	}

	/**
	 * 有序集合 添加元素
	 * @param key
	 * @param score
	 * @param member
	 * @return 1 新增 0 覆盖
	 */
	public Long zadd(String key, double score, String member) {
		return new Executor<Long>(shardedJedisPool) {

			@Override
			Long execute() {
				return jedis.zadd(key, score, member);
			}
		}.getResult();
	}

	/**
	 * 删除 一定区间内的元素
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public Long zremrangeByScore(String key, double start, double end) {
		return new Executor<Long>(shardedJedisPool) {
			@Override
			Long execute() {
				return jedis.zremrangeByScore(key, start, end);
			}
		}.getResult();
	}

	/**
	 * 删除 单个元素
	 * @param key
	 * @param members
	 * @return
	 */
	public Long zrem(String key, String... members) {
		return new Executor<Long>(shardedJedisPool) {
			@Override
			Long execute() {
				return jedis.zrem(key, members);}
		}.getResult();
	}


		/* ======================================Set====================================== */
	/**
	 * 判断set集合中是否存在该元素(value中是否存在一样的)
	 *
	 *	如果 member 元素是集合的成员，返回 1 。 true
	 *	如果 member 元素不是集合的成员，或 key 不存在，返回 0 。		false
	 * @param key	key值
	 * @param value	value值
	 * @return
	 */
	public Boolean ssismember(String key, String value) {
		return new Executor<Boolean>(shardedJedisPool) {
			@Override
			Boolean execute() {
				return jedis.sismember(key, value);}
		}.getResult();
	}

	/**
	 * 将一个或多个 member 元素加入到集合 key 当中，已经存在于集合的 member 元素将被忽略
	 *
	 *	如果 member 元素是集合的成员，返回 1 。 true
	 *	如果 member 元素不是集合的成员，或 key 不存在，返回 0 。		false
	 * @param key		key值
	 * @param members	元素
	 * @return
	 */
	public Long sadd(String key, String... members) {
		return new Executor<Long>(shardedJedisPool) {
			@Override
			Long execute() {
				return jedis.sadd(key, members);}
		}.getResult();
	}

	/* ======================================Other====================================== */

	/**
	 * 设置数据源
	 * @param shardedJedisPool 数据源
	 */
	public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {
		this.shardedJedisPool = shardedJedisPool;
	}

	/**
	 * 构造Pair键值对
	 * @param key key
	 * @param value value
	 * @return 键值对
	 */
	public <K, V> Pair<K, V> makePair(K key, V value) {
		return new Pair<K, V>(key, value);
	}

	/**
	 * 键值对
	 * @version V1.0
	 * @author fengjc
	 * @param <K> key
	 * @param <V> value
	 */
	public class Pair<K, V> {

		private K key;
		private V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public void setKey(K key) {
			this.key = key;
		}

		public V getValue() {
			return value;
		}

		public void setValue(V value) {
			this.value = value;
		}
	}

	/**
	 * 获取锁
	 * @param key
	 * @return
	 */
	public RedisLock initLock(String key) {
        return new Executor<RedisLock>(shardedJedisPool) {
            @Override
            RedisLock execute() {
                return new RedisLock(key, shardedJedisPool);
            }
        }.getResult();

	}
}

