---
name: kingdee_save
description: 保存金蝶ERP单据，支持新增和修改各类单据，包括采购订单、销售订单、收料通知单、生产订单等。保存后单据处于暂存或草稿状态，需要再提交和审核才能生效。
category: 单据操作
version: 1.1.0
endpoint:
  path: /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.Save.common.kdsvc
  auth: session
---

# 金蝶单据保存接口

## 请求体格式

```json
{
  "FormId": "单据表单ID",
  "Data": {
    "IsAutoSubmitAndAudit": false, // 是否自动提交并审核，默认 false
    "IsDeleteEntry": false,        // 更新时是否删除原明细行，默认 false
    "SubSystemId": "",
    "Model": {
      // 单据头字段
      "FDate": "2026-03-14",
      // 关联资料用 FNumber（编码）引用，不要用内码
      "FSupplierId": { "FNumber": "供应商编码" },
      // 明细行数组
      "POOrderEntry": [
        {
          "FMATERIALID": { "FNumber": "物料编码" },
          "FQty": 100,
          "FPrice": 10.5
        }
      ]
    }
  }
}
```

## 常用 FormId

| FormId | 单据名称 |
|--------|----------|
| `PUR_PurchaseOrder` | 采购订单 |
| `SAL_SaleOrder` | 销售订单 |
| `PUR_ReceiveBill` | 收料通知单 |
| `PRD_MO` | 生产订单 |
| `STK_TransferDirect` | 直接调拨单 |
| `STK_InStock` | 采购入库单 |

## 调用示例

**新增采购订单：**
```json
{
  "FormId": "PUR_PurchaseOrder",
  "Data": {
    "IsAutoSubmitAndAudit": false,
    "IsDeleteEntry": false,
    "SubSystemId": "",
    "Model": {
      "FDate": "2026-03-14",
      "FSupplierId": { "FNumber": "SUP001" },
      "FPurchaseDeptId": { "FNumber": "Dept01" },
      "POOrderEntry": [
        {
          "FMATERIALID": { "FNumber": "MAT001" },
          "FQty": 100,
          "FPrice": 10.5,
          "FTaxRateId": { "FNumber": "Tax13" },
          "FUnitId": { "FNumber": "Pcs" },
          "FDeliveryDate": "2026-03-31"
        }
      ]
    }
  }
}
```

## 返回格式

成功：
```json
{
  "Result": {
    "ResponseStatus": { "IsSuccess": true, "Errors": [] },
    "Id": "单据内码",
    "Number": "单据编号"
  }
}
```
失败：
```json
{
  "Result": {
    "ResponseStatus": {
      "IsSuccess": false,
      "Errors": [{ "Message": "错误描述" }]
    }
  }
}
```

## 注意

- 保存成功后单据为**草稿**状态，需再调用提交/审核接口才正式生效
- 更新已有单据须在 `Model` 中加入 `FID` 字段（通过 kingdee_query 查询获得）
