import httpx
from app.config import settings


async def wechat_code_to_session(code: str, platform: str = "miniprogram") -> dict:
    """微信小程序 code 换 session"""
    appid = settings.WECHAT_APPID if platform == "miniprogram" else settings.WECHAT_IOS_APPID
    secret = settings.WECHAT_SECRET if platform == "miniprogram" else settings.WECHAT_IOS_SECRET
    url = "https://api.weixin.qq.com/sns/jscode2session"
    async with httpx.AsyncClient() as client:
        resp = await client.get(url, params={
            "appid": appid,
            "secret": secret,
            "js_code": code,
            "grant_type": "authorization_code"
        })
        return resp.json()


async def wechat_ios_token(code: str) -> dict:
    """iOS 微信 SDK 授权 code 换 token"""
    # iOS 使用微信开放平台接口
    url = "https://api.weixin.qq.com/sns/oauth2/access_token"
    async with httpx.AsyncClient() as client:
        resp = await client.get(url, params={
            "appid": settings.WECHAT_IOS_APPID,
            "secret": settings.WECHAT_IOS_SECRET,
            "code": code,
            "grant_type": "authorization_code"
        })
        data = resp.json()
        if "access_token" in data:
            user = await client.get("https://api.weixin.qq.com/sns/userinfo", params={
                "access_token": data["access_token"],
                "openid": data["openid"]
            })
            return user.json()
        return data