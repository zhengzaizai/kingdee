# 金蝶webapi使用教学

## 1.url地址

完整url:    地址+service地址

地址获取方式:

![image-20260211153035727](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211153035727.png)

service地址:

| 功能     | 接口路径                                                     | 说明     |
| -------- | ------------------------------------------------------------ | -------- |
| 登录     | /Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc | 登录     |
| 查看     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc | 查看单据 |
| 查询     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc | 查询     |
| 查询     | /Kingdee.K3.SCM.WebApi.ServicesStub.ExpectQtyQueryWebApi.GetExpectQty.common.kdsvc | 可用量   |
| 报表查询 | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.GetSysReportData.common.kdsvc | 报表查询 |
| 暂存     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Draft.common.kdsvc | 暂存     |
| 保存     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc | 保存     |
| 批量保存 | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.BatchSave.common.kdsvc | 批量保存 |
| 提交     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Submit.common.kdsvc | 提交     |
| 审核     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Audit.common.kdsvc | 审核     |
| 下推     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Push.common.kdsvc | 下推     |
| 即时库存 | /Kingdee.K3.SCM.WebApi.ServicesStub.InventoryQueryService.GetInventoryData.common.kdsvc | 即时库存 |
| 可用量   | /Kingdee.K3.SCM.WebApi.ServicesStub.ExpectQtyQueryWebApi.GetExpectQty.common.kdsvc | 可用量   |
| 反审核   | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.UnAudit.common.kdsvc | 反审核   |
| 删除     | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Delete.common.kdsvc | 删除     |
| 分组查询 | /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc | 分组查询 |

## 2.获取token

访问上述登录接口:

| post            |                                                              |                                                              |
| --------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 接口参数：      |                                                              |                                                              |
| 参数列表        | 参数含义                                                     | 备注                                                         |
| acctID          | 账套Id，从管理中心数据库查询获得     参考查询：select FDATACENTERID from T_BAS_DATACENTER | 必须(用于区别正式环境测试环境)                               |
| username        | 用户登陆名                                                   | 必须                                                         |
| password        | 密码                                                         | 必须                                                         |
| lcid            | 语言id,选择哪种语言访问，参考：中文2052，英文1033，繁体3076  | 非必须，引用SDK组件辅助类调用则必须                          |
|                 |                                                              |                                                              |
| 返回参数：      |                                                              |                                                              |
| 参数列表        | 参数含义                                                     | 备注                                                         |
| LoginResultType | //激活     Activation = -7,     //云通行证未绑定Cloud账号     EntryCloudUnBind = -6,         //需要表单处理     DealWithForm = -5,     //登录警告     Wanning = -4,     //密码验证不通过（强制的）     PWInvalid_Required = -3,     //密码验证不通过（可选的）     PWInvalid_Optional = -2,     //登录失败     Failure = -1,     //用户或密码错误     PWError = 0,     //登录成功     Success = 1 | 管理员登陆可能出现返回-5的情况，这种情况在api验证时也可认为是允许的，具体可以根据实际情况来定。     var result =  JObject.Parse(ret)["LoginResultType"].Value<int>();     if (result == 1\|\|result==-5)     {     return true;     } |

| 请求体示例:                  |
| ---------------------------- |
| {                            |
| "acctID":  "68c1400c2779ce", |
| "username": "孙万",          |
| "password":  "kingdee2!",    |
| "lcid": 2052                 |
| }                            |

##### 上述参数获取方式:

 "username"& "password":大多数情况下使用管理员账号及其密码或有webapi调用权限的账号密码(由实施或客户自己提供)

"acctID":

![image-20260211154149230](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211154149230.png)

##### 返回参数示例:

注:此处部分信息用xxx替代

```json
{"Message":null,"MessageCode":"CheckPasswordPolicy","LoginResultType":1,"Context":{"UserLocale":"zh-CN","LogLocale":"zh-CN","DBid":"xxxx","DatabaseType":3,"SessionId":"n3xfizba4et0o2uutwdftvzg","UseLanguages":[{"LocaleId":2052,"LocaleName":"中文(简体)","Alias":"CN","LicenseType":0}],"UserId":178301,"UserName":"孙万","CustomName":"xxxxx","DisplayVersion":"9.0.553.10","DataCenterName":"xxxx","UserToken":"fedae6e6-bf7a-4229-b401-e6158a205f50","CurrentOrganizationInfo":{"ID":1,"AcctOrgType":"1","Name":"xxxx","FunctionIds":[101,102,103,104,108,106,107,109,110,111,112,113,114]},"IsCH_ZH_AutoTrans":false,"ClientType":32,"WeiboAuthInfo":{"WeiboUrl":null,"NetWorkID":null,"CompanyNetworkID":null,"Account":null,"TokenKey":null,"TokenSecret":null,"Verify":null,"CallbackUrl":null,"UserId":null},"UTimeZone":{"OffsetTicks":288000000000,"StandardName":"(UTC+08:00)北京，重庆，香港特别行政区，乌鲁木齐","Id":230,"Number":"1078_SYS","CanBeUsed":true},"STimeZone":{"OffsetTicks":288000000000,"StandardName":"(UTC+08:00)北京，重庆，香港特别行政区，乌鲁木齐","Id":230,"Number":"1078_SYS","CanBeUsed":true},"GDCID":"","Gsid":"5d8caae753bc4689a42","TRLevel":1,"ProductEdition":0,"DataCenterNumber":"xxxx","ContextResultType":0,"TenantId":"","IsDeployAsPublicCloud":false},
 
 "KDSVCSessionId":"2c6dfa6c-039b-4b65-b52f-898d3c7c522a",//需要的目标KDSVCSessionId
 
 "FormId":null,"RedirectFormParam":null,"FormInputObject":null,"ErrorStackTrace":null,"Lcid":0,"AccessToken":null,"CustomParam":{"FChkGUIOldMainConsle":false,"FChkEnabledSeqReq":false,"FIsDisabledGridRowCopy":false,"FIsDisabledCellSection":false,"FFieldDisabledShowBorder":false,"FListQuickFilterBackOld":false,"FImgFileCompress":false,"FSystemTipsRule":"0","FUnAllowTableColumnAutoHidden":false,"FChkToolBarFontLargeMode":false,"FUnChkEnabledSeqReq":false,"FCloseLRTips":false,"GlobalWatermarkConfigStr":"eyJ3aWR0aCI6MzAwLCJoZWlnaHQiOjE2MCwiYW5nbGUiOi0xNSwid2F0ZXJtYXJrdGV4dCI6IiAiLCJsaWN0ZXh0IjoiIiwib3BhY2l0eSI6MC4xLCJmb250ZmFtaWx5IjoiTWljcm9zb2Z0IFlhSGVpIiwiZm9udHNpemUiOjE0LCJiaWdfZm9udHNpemUiOjE2LCJzaG93dHlwZSI6MH0="},"KdAccessResult":null,"IsSuccessByAPI":true}
```

## 3.接口的调用格式

请求头部分:

| kdservice-sessionid | 登录取到的KDSVCSessionId |
| ------------------- | ------------------------ |
| Content-Type        | application/json         |

请求体部分:

```json
{
  "FormId": "PUR_PurchaseOrder", //formid填写对应单据的表单名称
  "Data": 
{
	//webapi接口生成的报文
}
```

"FormId"获取:

![image-20260211154812622](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211154812622.png)

## 4.webapi的使用

##### 1.webapi页面结构

此处以保存接口为例,通过更改操作列表选择保存审核查询等接口的参数说明

保存接口页面可以看到该单据所有参数的名称

![image-20260211161045562](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211161045562.png)

请求参数说明,用于控制参数很重要请仔细阅读!

(正常情况下这部分参数可以直接使用webapi返回的默认参数后续会提到)

![image-20260211160742798](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211160742798.png)

第二部分返回结果

第三部分代码示例

第四部分JSON格式数据就是放在data中的json参数格式

第五部分字段说明,是该接口每个字段的中英文对照

##### 2.如何获取目标报文(以保存接口为例)

找到目的单据,点击在线测试WebAPI,输入密码,点击验证接口

![image-20260211161901129](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211161901129.png)

在接口列表选择目标接口,点击填写测试数据,会出现页面,能够仿照erp前端操作员直接制单,填写完毕,点击返回数据,可以在测试数据窗口得到目标json,点击验证接口可以在详细结果中看到该报文返回的详细结构保存成功与否

注:也可以将已有的报文粘贴到测试数据窗口中点击验证接口进行测试

![image-20260211162252611](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211162252611.png)

注:不同的接口填写测试数据的方式也不一样

例如查询接口

![image-20260211162854966](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211162854966.png)

## 5.重要接口说明

#### 1.查询

| 请求头:                                                  |                                                              |
| -------------------------------------------------------- | ------------------------------------------------------------ |
| kdservice-sessionid                                      | 登录取到的KDSVCSessionId                                     |
| Content-Type                                             | application/json                                             |
|                                                          |                                                              |
| **请求体示例:**                                          |                                                              |
| (以采购入库为例查询上游采购订单)注本项目上游为收料通知单 |                                                              |
| 1.根据采购订单号(FBillNo)查询目标采购订单内码            |                                                              |
| {                                                        | 说明                                                         |
| "FormId": "PUR_PurchaseOrder",                           | 表单名称(采购订单)                                           |
| "Data":                                                  |                                                              |
| {                                                        |                                                              |
| "FormId": "PUR_PurchaseOrder",                           | 表单名称                                                     |
| "FieldKeys": "FID,FPOOrderEntry_FEntryID ",              | 目标字段(FID单据内码,FPOOrderEntry_FEntryID采购订单的明细行内码 |
| "FilterString": "FBillNo='1111'",                        | 查询条件(查询条件为采购订单编号=1111)                        |
| "OrderString": "",                                       |                                                              |
| "TopRowCount": 0,                                        |                                                              |
| "StartRow": 0,                                           |                                                              |
| "Limit": 2000,                                           |                                                              |
| "SubSystemId": ""                                        |                                                              |
| }                                                        |                                                              |

##### 参数查询说明:

###### 1.明细行内码查询格式

明细表名_实体主键(每个单据都不同)

此处为FEntity_FEntryID

![image-20260211171309295](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211171309295.png)

###### 2.部分字段的名称(FName)与编码(FNumber)存在于基础资料表中

可在保存接口json中看到如下格式

```json
          "FParentOwnerId": {
                "FNumber": ""
            },
            "FSrcBizBillType": {
                "FNAME": ""
            },
```
FParentOwnerId,FSrcBizBillType这样的参数的编码名称就是放在基础资料表里的

查询名称时需要查询  FSrcBizBillType.FNAME ,编码需要查询 FSrcBizBillType.FNumber   (直接查询FSrcBizBillType得到的是内码)

###### 3.仓库,仓位集值,仓位



#### 2.下推

对应需要进行上下游单据关联的单据我们通常推荐下推后再调用保存接口进行更新

#### 3.保存/更新

## 6.关于如何在保存时对单据进行上下游关联

说明:使用下推后进行更新保存,若是在下推后得到的明细行上进行修改则该明细自带关联关系无需修改

但是若下推后进行更新参数"IsDeleteEntry": "true",删除了原明细行后再进行保存则新生成的明细行不带关联

关联关系表是带在下游单据明细行里的请注意!

推荐参考博客:  https://vip.kingdee.com/article/171055?productLineId=1&lang=zh-CN (博客更详细)

![image-20260211164039119](C:\Users\ws\AppData\Roaming\Typora\typora-user-images\image-20260211164039119.png)

示例json   !!注意,每个单据的关联关系表名和字段名都是不一样的!

```json
"FEntity_Link": [
                    {
                        "FEntity_Link_FRuleId": "xxxxx",//固定参数,有需要可以问erp实施或开发提供
                        "FEntity_Link_FSTableName": "xxx",
                        //固定参数,源单表名正常情况就是上游单据的formid,某些时候会是erp内部表名
                        
                        //*建议前端先手动下推一个单据,然后在调用查询接口查询上面这两个参数
                        
                        "FEntity_Link_FSBillId": "100008",//源单内码,既上游单据的FID
                        "FEntity_Link_FSId": "100008"//源单分录内码,既上游明细行的内码
                    }
                ]
```

构造好后把 FEntity_Link 当成一个参数塞进对应明细行的json体中即可

# 特殊问题解决记录

###### 1.**更新库存时出现可以忽略的异常数据，是否继续？**

InterationFlags:"STK_InvCheckResult"