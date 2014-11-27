package com.foxinmy.weixin4j.mp.api;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.foxinmy.weixin4j.exception.WeixinException;
import com.foxinmy.weixin4j.http.JsonResult;
import com.foxinmy.weixin4j.http.Response;
import com.foxinmy.weixin4j.model.Token;
import com.foxinmy.weixin4j.msg.model.Base;
import com.foxinmy.weixin4j.msg.model.Massable;
import com.foxinmy.weixin4j.msg.model.MpArticle;
import com.foxinmy.weixin4j.msg.model.MpNews;
import com.foxinmy.weixin4j.msg.model.Video;
import com.foxinmy.weixin4j.token.TokenHolder;

/**
 * 群发相关API
 * 
 * @className MassApi
 * @author jy.hu
 * @date 2014年9月25日
 * @since JDK 1.7
 * @see <a
 *      href="http://mp.weixin.qq.com/wiki/index.php?title=%E9%AB%98%E7%BA%A7%E7%BE%A4%E5%8F%91%E6%8E%A5%E5%8F%A3">群发接口</a>
 * @see com.foxinmy.weixin4j.msg.model.MpArticle
 */
public class MassApi extends BaseApi {

	private final TokenHolder tokenHolder;

	public MassApi(TokenHolder tokenHolder) {
		this.tokenHolder = tokenHolder;
	}

	/**
	 * 上传图文消息,一个图文消息支持1到10条图文</br> <font
	 * color="red">具备微信支付权限的公众号，在使用高级群发接口上传、群发图文消息类型时，可使用&lta&gt标签加入外链</font>
	 * 
	 * @param articles
	 *            图片消息
	 * @return 媒体ID
	 * @throws WeixinException
	 * @see <a
	 *      href="http://mp.weixin.qq.com/wiki/index.php?title=%E9%AB%98%E7%BA%A7%E7%BE%A4%E5%8F%91%E6%8E%A5%E5%8F%A3">高级群发</a>
	 * @see <a
	 *      href="http://mp.weixin.qq.com/wiki/index.php?title=%E9%AB%98%E7%BA%A7%E7%BE%A4%E5%8F%91%E6%8E%A5%E5%8F%A3#.E4.B8.8A.E4.BC.A0.E5.9B.BE.E6.96.87.E6.B6.88.E6.81.AF.E7.B4.A0.E6.9D.90">上传图文消息</a>
	 * @see com.foxinmy.weixin4j.msg.model.MpArticle
	 */
	public String uploadArticle(List<MpArticle> articles)
			throws WeixinException {
		String article_upload_uri = getRequestUri("article_upload_uri");
		Token token = tokenHolder.getToken();
		JSONObject obj = new JSONObject();
		obj.put("articles", articles);
		Response response = request.post(
				String.format(article_upload_uri, token.getAccessToken()),
				obj.toJSONString());

		return response.getAsJson().getString("media_id");
	}

	/**
	 * 上传分组群发的视频素材
	 * 
	 * @param video
	 *            视频对象 其中mediaId媒体文件中上传得到的Id 不能为空
	 * @return 上传后的ID 可用于群发视频消息
	 * @throws WeixinException
	 * @see <a
	 *      href="http://mp.weixin.qq.com/wiki/index.php?title=%E9%AB%98%E7%BA%A7%E7%BE%A4%E5%8F%91%E6%8E%A5%E5%8F%A3">高级群发</a>
	 * @see com.foxinmy.weixin4j.msg.model.Video
	 * @see com.foxinmy.weixin4j.msg.model.MpVideo
	 * @see {@link com.foxinmy.weixin4j.mp.api.MediaApi#uploadMedia(File)}
	 */
	public String uploadVideo(Video video) throws WeixinException {
		String video_upload_uri = getRequestUri("video_upload_uri");
		Token token = tokenHolder.getToken();
		Response response = request.post(
				String.format(video_upload_uri, token.getAccessToken()),
				JSON.toJSONString(video));

		return response.getAsJson().getString("media_id");
	}

	/**
	 * 分组群发
	 * <p>
	 * 在返回成功时,意味着群发任务提交成功,并不意味着此时群发已经结束,所以,仍有可能在后续的发送过程中出现异常情况导致用户未收到消息,
	 * 如消息有时会进行审核、服务器不稳定等,此外,群发任务一般需要较长的时间才能全部发送完毕
	 * </p>
	 * 
	 * @param box
	 *            消息项
	 * @param groupId
	 *            分组ID
	 * @return 群发后的消息ID
	 * @throws WeixinException
	 * @see com.foxinmy.weixin4j.mp.model.Group
	 * @see com.foxinmy.weixin4j.msg.model.Text
	 * @see com.foxinmy.weixin4j.msg.model.Image
	 * @see com.foxinmy.weixin4j.msg.model.Voice
	 * @see com.foxinmy.weixin4j.msg.model.MpVideo
	 * @see com.foxinmy.weixin4j.msg.model.MpNews
	 * @see {@link com.foxinmy.weixin4j.mp.api.MediaApi#uploadMedia(File)}
	 * @see {@link com.foxinmy.weixin4j.mp.api.GroupApi#getGroups()}
	 */
	public String massByGroupId(Base box, int groupId) throws WeixinException {
		if (box instanceof MpNews) {
			MpNews _news = (MpNews) box;
			List<MpArticle> _articles = _news.getArticles();
			if (StringUtils.isBlank(_news.getMediaId()) && _articles != null
					&& !_articles.isEmpty()) {
				return massArticleByGroupId(_articles, groupId);
			}
		}
		if (!(box instanceof Massable)) {
			throw new WeixinException("-1", String.format(
					"%s not implement Massable", box.getClass()));
		}
		String msgtype = box.getMediaType().name();
		JSONObject obj = new JSONObject();
		JSONObject item = new JSONObject();
		item.put("group_id", groupId);
		obj.put("filter", item);
		obj.put(msgtype, JSON.toJSON(box));
		obj.put("msgtype", msgtype);
		String mass_group_uri = getRequestUri("mass_group_uri");
		Token token = tokenHolder.getToken();
		Response response = request.post(
				String.format(mass_group_uri, token.getAccessToken()),
				obj.toJSONString());

		return response.getAsJson().getString("msg_id");
	}

	/**
	 * 分组ID群发图文消息
	 * 
	 * @param articles
	 *            图文列表
	 * @param openIds
	 *            openId列表
	 * @return 群发后的消息ID
	 * @see {@link com.foxinmy.weixin4j.mp.api.MassApi#massByGroupId(Base,int)}
	 * @throws WeixinException
	 */
	public String massArticleByGroupId(List<MpArticle> articles, int groupId)
			throws WeixinException {
		String mediaId = uploadArticle(articles);
		return massByGroupId(new MpNews(mediaId), groupId);
	}

	/**
	 * openId群发
	 * 
	 * @param box
	 *            消息项
	 * @param openIds
	 *            openId列表
	 * @return 群发后的消息ID
	 * @throws WeixinException
	 * @see com.foxinmy.weixin4j.mp.model.User
	 * @see com.foxinmy.weixin4j.msg.model.Text
	 * @see com.foxinmy.weixin4j.msg.model.Image
	 * @see com.foxinmy.weixin4j.msg.model.Voice
	 * @see com.foxinmy.weixin4j.msg.model.MpVideo
	 * @see com.foxinmy.weixin4j.msg.model.MpNews
	 * @see {@link com.foxinmy.weixin4j.mp.api.MediaApi#uploadMedia(File)}
	 * @see {@link com.foxinmy.weixin4j.mp.api.UserApi#getUser(String)}
	 */
	public String massByOpenIds(Base box, String... openIds)
			throws WeixinException {
		if (box instanceof MpNews) {
			MpNews _news = (MpNews) box;
			List<MpArticle> _articles = _news.getArticles();
			if (StringUtils.isBlank(_news.getMediaId()) && _articles != null
					&& !_articles.isEmpty()) {
				return massArticleByOpenIds(_articles, openIds);
			}
		}
		if (!(box instanceof Massable)) {
			throw new WeixinException("-1", String.format(
					"%s not implement Massable", box.getClass()));
		}
		String msgtype = box.getMediaType().name();
		JSONObject obj = new JSONObject();
		obj.put("touser", openIds);
		obj.put(msgtype, JSON.toJSON(box));
		obj.put("msgtype", msgtype);
		String mass_openid_uri = getRequestUri("mass_openid_uri");
		Token token = tokenHolder.getToken();
		Response response = request.post(
				String.format(mass_openid_uri, token.getAccessToken()),
				obj.toJSONString());

		return response.getAsJson().getString("msg_id");
	}

	/**
	 * 根据openid群发图文消息
	 * 
	 * @param articles
	 *            图文列表
	 * @param openIds
	 *            openId列表
	 * @return 群发后的消息ID
	 * @see {@link com.foxinmy.weixin4j.mp.api.MassApi#massByOpenIds(Base,String...)}
	 * @throws WeixinException
	 */
	public String massArticleByOpenIds(List<MpArticle> articles,
			String... openIds) throws WeixinException {
		String mediaId = uploadArticle(articles);
		return massByOpenIds(new MpNews(mediaId), openIds);
	}

	/**
	 * 删除群发消息
	 * <p>
	 * 请注意,只有已经发送成功的消息才能删除删除消息只是将消息的图文详情页失效,已经收到的用户,还是能在其本地看到消息卡片
	 * </p>
	 * 
	 * @param msgid
	 *            发送出去的消息ID
	 * @throws WeixinException
	 * @see <a
	 *      href="http://mp.weixin.qq.com/wiki/index.php?title=%E9%AB%98%E7%BA%A7%E7%BE%A4%E5%8F%91%E6%8E%A5%E5%8F%A3#.E5.88.A0.E9.99.A4.E7.BE.A4.E5.8F.91">删除群发</a>
	 * @see {@link com.foxinmy.weixin4j.mp.api.MassApi#massByGroup(Base, int)}
	 * @see {@link com.foxinmy.weixin4j.mp.api.MassApi#massByOpenIds(Base, String...)
	 */
	public JsonResult deleteMassNews(String msgid) throws WeixinException {
		JSONObject obj = new JSONObject();
		obj.put("msgid", msgid);
		String mass_delete_uri = getRequestUri("mass_delete_uri");
		Token token = tokenHolder.getToken();
		Response response = request.post(
				String.format(mass_delete_uri, token.getAccessToken()),
				obj.toJSONString());

		return response.getAsJsonResult();
	}
}