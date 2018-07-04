package com.liang.service.interfaces;

import com.liang.dto.Exposer;
import com.liang.dto.SeckillExecution;
import com.liang.entity.Seckill;
import com.liang.exception.RepeatKillException;
import com.liang.exception.SecKillException;

import java.util.List;

public interface SecKillService {

    /**
     * 查询全部记录
     * @return
     */
    List<Seckill> getSecKillList();

    /**
     * 查询单个记录
     * @param seckillId
     * @return
     */
    Seckill getById(long seckillId);

    /**
     * @param seckillId 商品的id
     * @return 根据对应的状态返回对应的状态实体
     */
    Exposer exportSeckillUrl(long seckillId);

    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SecKillException, RepeatKillException, SecKillException;

    SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5);
}
