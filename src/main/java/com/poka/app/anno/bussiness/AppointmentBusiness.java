package com.poka.app.anno.bussiness;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.poka.app.anno.base.service.impl.OrderInfoService;
import com.poka.app.anno.enity.OrderInfo;
import com.poka.app.enumtype.OrderType;
import com.poka.app.enumtype.StateType;
import com.poka.app.pb.ws.IPBPospSW;
import com.poka.app.util.CxfUtil;
import com.poka.app.util.PokaDateUtil;
import com.poka.app.vo.AppointmenResult;
import com.poka.app.vo.AppointmentVo;

@Component
public class AppointmentBusiness {
	Logger logger = Logger.getLogger(AppointmentBusiness.class);
	private OrderInfoService orderInfoService;
	
	private CxfUtil cxfUtil;

	@Autowired
	public void setCxfUtil(CxfUtil cxfUtil) {
		this.cxfUtil = cxfUtil;
	}

	@Autowired
	@Qualifier("orderInfoService")
	public void setOrderInfoService(OrderInfoService orderInfoService) {
		this.orderInfoService = orderInfoService;
	}

	/**
	 * 预约取款
	 */
	public void makeAppointment() {
		List<OrderInfo> orders = this.orderInfoService
				.getUnsendOrder(OrderType.APPOINTMENT);
		if (orders.size() <= 0) {
			return;
		}
		logger.info("未处理預約取款订单数量为:" + orders.size());
		for (OrderInfo order : orders) {
			String orderId = order.getOrderId().trim();
			logger.info("处理預約取款订单号:" + orderId);
			AppointmentVo vo = this.orderInfoService.getAppointmentVo(orderId);

			if (vo.getReserveMain() == null || vo.getReserveDetails() == null
					|| vo.getReserveDetails().size() == 0) {
				logger.info("continue:" + orderId);
				continue;
			}
			IPBPospSW service = cxfUtil.getCxfClient(IPBPospSW.class, cxfUtil.getUrl());
			cxfUtil.recieveTimeOutWrapper(service);
		
			boolean result = Boolean.FALSE;
			try{
				result = service.makeAppointmen(vo);
			}catch(Exception ex){
				logger.info("连接服务器失败...**"+PokaDateUtil.getNow()+"**");
			}
			if (result) {
				
				logger.info("处理預約取款订单:" + orderId+"  成功...**[执行时间：" + PokaDateUtil.getNow() + "]**");
				this.orderInfoService.updateOrderInfoState(order,
						StateType.SENDED);
			}else{
				logger.info("处理預約取款订单:" + orderId+"  失败...**[执行时间：" + PokaDateUtil.getNow() + "]**");
			}
			try {
				Thread.sleep(50000);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
	}

	public boolean handleAppointment(AppointmenResult appointment) {
		return orderInfoService.saveAppointmentResult(appointment);
	}
}
