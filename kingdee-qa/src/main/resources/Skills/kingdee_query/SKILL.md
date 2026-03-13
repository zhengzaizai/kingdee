---
name: kingdee_query
description: 查询金蝶ERP系统即时库存数据。支持按物料名称、物料编码、仓库名称、仓库编码等多种条件查询库存数量和仓库信息。
category: 库存查询
version: 2.0.0
---

# 金蝶即时库存查询

## 工具名称
`execute`

## 固定参数

即时库存查询时，`formId` 固定为：
```
STK_Inventory
```

---

## 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| formId | string | 是 | 固定填 `STK_Inventory` |
| fieldKeys | string | 是 | 需要返回的字段，多个用英文逗号分隔 |
| filterString | string | 否 | 过滤条件，OQL语法，字符串值用**单引号** |
| orderString | string | 否 | 排序，如 `FMATERIALID.FNumber`，不需要传空字符串 |
| limit | int | 否 | 最大返回行数，默认100，最大10000 |

---

## fieldKeys 常用字段

| 字段 | 含义 |
|------|------|
| `FMATERIALID.FName` | 物料名称（必须加 .FName，否则只得到内码数字）|
| `FMATERIALID.FNumber` | 物料编码 |
| `FStockId.FName` | 仓库名称（必须加 .FName）|
| `FStockId.FNumber` | 仓库编码 |
| `FBaseQty` | 基本单位库存数量 |
| `FQty` | 库存数量 |
| `FStockLocId.FName` | 仓位名称（如需细化到仓位）|

**推荐默认 fieldKeys：**
```
FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FStockId.FNumber,FBaseQty
```

---

## 用户意图 → filterString 映射

| 用户描述 | filterString |
|----------|--------------|
| 按物料名称（含模糊查询） | `FMATERIALID.FName like '%腰托%'` |
| 按物料编码（精确）| `FMATERIALID.FNumber='M001'` |
| 按仓库名称（含模糊）| `FStockId.FName like '%成品仓%'` |
| 按仓库编码（精确）| `FStockId.FNumber='001'` |
| 某物料在某仓库（组合）| `FMATERIALID.FName like '%腰托%' and FStockId.FName like '%成品仓%'` |
| 不筛选，查全部 | 传空字符串 `""` |

**重要：** filterString 中所有字符串值必须用**单引号**括起来，如 `'腰托'`，不能用双引号。

---

## 完整调用示例

### 按物料名称查库存（最常用）
用户说："腰托在哪个仓库" / "查一下螺丝的库存"
```
formId      = "STK_Inventory"
fieldKeys   = "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty"
filterString= "FMATERIALID.FName like '%腰托%'"
orderString = ""
limit       = 100
```

### 按物料编码查库存
用户说："查一下编号 M001 的库存"
```
formId      = "STK_Inventory"
fieldKeys   = "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty"
filterString= "FMATERIALID.FNumber='M001'"
orderString = ""
limit       = 100
```

### 按仓库查所有库存
用户说："成品仓有哪些物料"
```
formId      = "STK_Inventory"
fieldKeys   = "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty"
filterString= "FStockId.FName like '%成品仓%'"
orderString = "FMATERIALID.FNumber"
limit       = 200
```

### 查询全部库存
用户说："查一下所有库存"
```
formId      = "STK_Inventory"
fieldKeys   = "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty"
filterString= ""
orderString = "FMATERIALID.FNumber"
limit       = 500
```

---

## 返回结果格式

成功：
```json
{
  "success": true,
  "total": 3,
  "count": 3,
  "records": [
    {"FMATERIALID.FName": "腰托A型", "FMATERIALID.FNumber": "MT001", "FStockId.FName": "成品仓", "FBaseQty": 100.0},
    {"FMATERIALID.FName": "腰托B型", "FMATERIALID.FNumber": "MT002", "FStockId.FName": "原材料仓", "FBaseQty": 50.0}
  ]
}
```

失败：
```json
{"success": false, "error": "错误信息"}
```

---

## 注意事项

1. 直接查询 `FMATERIALID`（不加 `.FName`）只能得到内码数字，不是名称
2. filterString 为空字符串 `""` 时查询全部库存，数量可能很大，建议配合 limit
3. limit 最大 10000，建议设 100~500
4. 若返回 `success=false` 且 error 含 "上下文丢失"，说明登录 Session 已过期，需重启程序重新登录
