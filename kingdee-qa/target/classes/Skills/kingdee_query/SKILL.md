---
name: kingdee_query
description: 查询金蝶ERP系统单据数据。支持即时库存、采购订单、销售订单、物料、供应商、客户、仓库等各类数据查询。使用 callApi 工具调用，skillName 填 kingdee_query。
category: 数据查询
version: 3.3.0
endpoint:
  path: /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc
  auth: session
---

# 金蝶单据查询接口

## 请求体格式

```json
{
  "FormId": "表单ID",
  "Data": {
    "FormId": "表单ID",        // 与外层一致
    "FieldKeys": "字段1,字段2", // 要查询的字段，逗号分隔
    "FilterString": "",        // 过滤条件，空字符串=不过滤
    "OrderString": "",         // 排序。常用值："FBaseQty desc"(库存降序) "FBaseQty asc"(库存升序) "FDate desc"(日期降序) "FDate asc"(日期升序) "FAmount desc"(金额降序)，多字段用逗号分隔
    "TopRowCount": 0,           // 0=不限制
    "StartRow": 0,              // 分页起始行
    "Limit": 100,               // 返回行数上限，建议100~500，最大10000
    "SubSystemId": ""
  }
}
```

## 常用 FormId

| FormId | 说明 |
|--------|------|
| `STK_Inventory` | 即时库存 |
| `PUR_PurchaseOrder` | 采购订单 |
| `SAL_SaleOrder` | 销售订单 |
| `BD_Material` | 物料资料 |
| `BD_Supplier` | 供应商 |
| `BD_Customer` | 客户 |
| `BD_StockPlace` | 仓库 |
| `STK_InStock` | 采购入库单 |

## 常用 FieldKeys

**即时库存（STK_Inventory）推荐：**
```
FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty,FLot.FNumber
```
- `FMATERIALID.FName` 物料名称（必须加 `.FName`，否则只得到内码）
- `FMATERIALID.FNumber` 物料编码
- `FStockId.FName` 仓库名称（必须加 `.FName`）
- `FBaseQty` 基本单位库存数量
- `FLot.FNumber` 批号（需要按批号查询或显示批号时加上）

**采购订单（PUR_PurchaseOrder）推荐：**
```
FBillNo,FDate,FSupplierId.FName,FMATERIALID.FName,FQty,FAmount,FDocumentStatus
```

## FilterString 语法

- 字符串值用**单引号**，不能用双引号
- **精确查询**（完全匹配）：`FMATERIALID.FName='腰托'` - 只查找名称完全等于"腰托"的物料
- **模糊查询**（包含匹配）：`FMATERIALID.FName like '%腰托%'` - 查找名称中包含"腰托"的所有物料
- **编码精确查询**：`FMATERIALID.FNumber='M001'` - 按物料编码精确查找
- **批号精确查询**：`FLot.FNumber='250210'` - 按批号精确查找
- **批号模糊查询**：`FLot.FNumber like '%250%'` - 按批号模糊查找
- **批号+物料组合**：`FMATERIALID.FName='腰托' and FLot.FNumber='250210'`
- 仓库过滤：`FStockId.FName='1F_成品库'`
- 组合：`FMATERIALID.FName like '%腰托%' and FStockId.FName like '%成品%'`
- 日期：`FDate>='2026-01-01' and FDate<='2026-03-31'`
- 数量筛选：`FBaseQty>0`（仅返回有库存的记录）

### 查询类型选择指南
- **默认使用精确查询**：用户说物料名称时，默认用 `FMATERIALID.FName='腰托'`
- 仅当用户明确说"包含"、"相关"、"有XX的"、"名字里有XX"时，才用模糊查询 `like '%XX%'`
- 用户提供完整物料编码时用编码精确查询：`FMATERIALID.FNumber='M001'`
- 用户说批号时用批号查询：`FLot.FNumber='批号值'`
- **排名类问题（最多/最少/前N名）**：使用 OrderString 排序配合 Limit 限制返回条数。库存最多：OrderString=FBaseQty desc + Limit=1；库存前10：OrderString=FBaseQty desc + Limit=10；最新订单：OrderString=FDate desc + Limit=N
- **全仓库排名类问题**：去掉仓库过滤，用 OrderString 按 FBaseQty desc 排序，Limit 设为需要的数量

---

## 即时库存调用示例（重点）

### 示例1：查询某物料在所有仓库的库存（精确）
用户问："腰托在哪些仓库？" / "腰托的库存情况"
```json
{
  "FormId": "STK_Inventory",
  "Data": {
    "FormId": "STK_Inventory",
    "FieldKeys": "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty",
    "FilterString": "FMATERIALID.FName='腰托'",
    "OrderString": "",
    "TopRowCount": 0,
    "StartRow": 0,
    "Limit": 200,
    "SubSystemId": ""
  }
}
```

---

### 示例2：按批号查询物料
用户问："批号250210是什么物料？" / "250210这批货在哪？"
```json
{
  "FormId": "STK_Inventory",
  "Data": {
    "FormId": "STK_Inventory",
    "FieldKeys": "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty,FLot.FNumber",
    "FilterString": "FLot.FNumber='250210'",
    "OrderString": "",
    "TopRowCount": 0,
    "StartRow": 0,
    "Limit": 100,
    "SubSystemId": ""
  }
}
```

---

### 示例3：批号+物料名组合查询
用户问："腰托250210这批在哪个仓库？"
```json
{
  "FormId": "STK_Inventory",
  "Data": {
    "FormId": "STK_Inventory",
    "FieldKeys": "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty,FLot.FNumber",
    "FilterString": "FMATERIALID.FName='腰托' and FLot.FNumber='250210'",
    "OrderString": "",
    "TopRowCount": 0,
    "StartRow": 0,
    "Limit": 100,
    "SubSystemId": ""
  }
}
```

---

### 示例4：查某仓库下所有物料
用户问："1F_成品库都有什么物料？"
```json
{
  "FormId": "STK_Inventory",
  "Data": {
    "FormId": "STK_Inventory",
    "FieldKeys": "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty",
    "FilterString": "FStockId.FName='1F_成品库'",
    "OrderString": "",
    "TopRowCount": 0,
    "StartRow": 0,
    "Limit": 500,
    "SubSystemId": ""
  }
}
```

---

### 示例5：查全仓库库存最多的物料
用户问："哪个物料数量最多？" / "库存最多的是什么？"
```json
{
  "FormId": "STK_Inventory",
  "Data": {
    "FormId": "STK_Inventory",
    "FieldKeys": "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty",
    "FilterString": "FBaseQty>0",
    "OrderString": "",
    "TopRowCount": 0,
    "StartRow": 0,
    "Limit": 2000,
    "SubSystemId": ""
  }
}
```
> **必须**用大 Limit 取全量，禁止用 OrderString 排序。拿到数据后 AI 自行按 FBaseQty 找最大值，同一物料多仓库需合并累加。

---

### 示例6：查指定物料在指定仓库的精确库存
用户问："气泵阀组分总成在1F_成品库有多少？"
```json
{
  "FormId": "STK_Inventory",
  "Data": {
    "FormId": "STK_Inventory",
    "FieldKeys": "FMATERIALID.FName,FMATERIALID.FNumber,FStockId.FName,FBaseQty",
    "FilterString": "FMATERIALID.FName='气泵阀组分总成' and FStockId.FName='1F_成品库'",
    "OrderString": "",
    "TopRowCount": 0,
    "StartRow": 0,
    "Limit": 100,
    "SubSystemId": ""
  }
}
```

---

## 其他单据调用示例

### 查采购订单（按日期过滤）
```json
{
  "FormId": "PUR_PurchaseOrder",
  "Data": {
    "FormId": "PUR_PurchaseOrder",
    "FieldKeys": "FBillNo,FDate,FSupplierId.FName,FMATERIALID.FName,FQty,FAmount,FDocumentStatus",
    "FilterString": "FDate>='2026-03-01' and FDate<='2026-03-31'",
    "OrderString": "FDate desc",
    "TopRowCount": 0,
    "StartRow": 0,
    "Limit": 50,
    "SubSystemId": ""
  }
}
```

---

## Limit 选择参考

| 场景 | 建议 Limit |
|------|------------|
| 精确查单个物料+单个仓库 | 100 |
| 按批号查询 | 100 |
| 查某仓库所有物料 | 500 |
| 查某物料所有仓库 | 200 |
| 全库排名（最多/最少） | 1000~2000 |
| 采购/销售订单列表 | 50~100 |

---

## 返回格式

```json
{
  "success": true,
  "total": 3,
  "count": 3,
  "records": [
    {"FMATERIALID.FName": "腰托", "FStockId.FName": "1F_成品库", "FBaseQty": 94.0, "FLot.FNumber": "250210"}
  ]
}
```
失败：`{"success": false, "error": "错误信息"}`
