import great_expectations as gx
import pandas as pd
import json

# 1. Load dữ liệu
df = pd.read_csv("data/test_data.csv")
context = gx.get_context()

# 2. Đọc và chuẩn hóa Suite từ JSON
with open("app/rules/transaction_suite.json", "r") as f:
    suite_json = json.load(f)

# Map lại format cho bản 1.x
expectations = []
for exp in suite_json.get("expectations", []):
    expectations.append({
        "type": exp["expectation_type"],
        "kwargs": exp["kwargs"]
    })

# 3. Tạo Suite
suite = gx.ExpectationSuite(
    name="transaction_suite_v1",
    expectations=expectations
)

# 4. Sử dụng Validator với batch_data (Cách an toàn nhất cho Pandas)
# Chúng ta dùng 'runtime_parameters' để truyền DataFrame trực tiếp
validator = context.get_validator(
    datasource_name=context.data_sources.add_pandas(name="my_ds_unique").name,
    data_asset_name=context.data_sources.get("my_ds_unique").add_dataframe_asset(name="my_asset").name,
    expectation_suite=suite,
)

# 5. THỰC THI (Dùng phương thức validate của Validator, không phải của Asset)
results = validator.validate()

# 6. IN KẾT QUẢ
print(f"\n" + "="*30)
print(f"TRẠNG THÁI: {'ĐẠT' if results.success else 'KHÔNG ĐẠT'}")
print(f"Tỷ lệ thành công: {results.statistics['success_percent']:.1f}%")
print("="*30)

# 7. CHI TIẾT LỖI
print("\n--- CHI TIẾT DÒNG LỖI ---")
for res in results.results:
    if not res.success:
        rule = res.expectation_config.type
        bad_values = res.result.get('partial_unexpected_list', [])
        print(f"❌ Quy tắc '{rule}' thất bại. Các giá trị lỗi: {bad_values}")