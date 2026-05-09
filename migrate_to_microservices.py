import os
import shutil

# --- 配置区 ---
SOURCE_ROOT = "backend/src"
TARGET_ROOT = "." 
PACKAGE_PATH = "com/vue/readingapp"

# 映射表: {"原文件夹名": "目标微服务模块名"}
MAPPING = {
    # Auth
    "auth": "auth-service",
    
    # User 相关
    "user": "user-service",
    "feedback": "user-service",
    "notifications": "user-service",
    "settings": "user-service",
    
    # Document 相关
    "documents": "document-service",
    "ocr": "document-service",
    "reader": "document-service",
    "search": "document-service",
    "offline": "document-service",
    "export": "document-service",
    
    # Study 相关
    "vocabulary": "study-service",
    "review": "study-service",
    "scheduler": "study-service",
    
    # 公共/底层相关
    "tags": "common-api",
    "config": "common-api",
    "system": "common-api",
}

def migrate_item(src_type, item_name, service):
    """
    src_type: 'main/java' 或 'test/java'
    item_name: 文件夹名或文件名
    service: 目标微服务名
    """
    # 原路径: backend/src/main/java/com/vue/readingapp/auth
    src_path = os.path.join(SOURCE_ROOT, src_type, PACKAGE_PATH, item_name)
    # 目标路径: auth-service/src/main/java/com/vue/readingapp/auth
    dest_path = os.path.join(TARGET_ROOT, service, "src", src_type, PACKAGE_PATH, item_name)

    if not os.path.exists(src_path):
        return False

    # 创建目标父目录
    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
    
    print(f"[迁移] {src_path} -> {dest_path}")

    if os.path.isdir(src_path):
        # 处理文件夹
        if os.path.exists(dest_path):
            for item in os.listdir(src_path):
                s = os.path.join(src_path, item)
                d = os.path.join(dest_path, item)
                if os.path.isdir(s):
                    shutil.copytree(s, d, dirs_exist_ok=True)
                else:
                    shutil.copy2(s, d)
        else:
            shutil.copytree(src_path, dest_path)
    else:
        # 处理单个文件 (如 ReadingAppApplication.java)
        shutil.copy2(src_path, dest_path)
    
    return True

def migrate():
    # 1. 迁移映射表中的文件夹
    for folder, service in MAPPING.items():
        # 迁移 main 代码
        migrate_item("main/java", folder, service)
        # 迁移 test 代码
        migrate_item("test/java", folder, service)
        # 迁移 resources (可选，如果 resources 下也有相同结构的文件夹)
        # migrate_item("main/resources", folder, service)

    # 2. 处理包根目录下的 Java 文件 (如 ReadingAppApplication.java)
    # 获取源包根目录下的所有文件
    root_java_dir = os.path.join(SOURCE_ROOT, "main/java", PACKAGE_PATH)
    if os.path.exists(root_java_dir):
        for file in os.listdir(root_java_dir):
            file_path = os.path.join(root_java_dir, file)
            if os.path.isfile(file_path) and file.endswith(".java"):
                # 默认将启动类移至 user-service 或你指定的模块
                migrate_item("main/java", file, "user-service")

if __name__ == "__main__":
    print("开始执行业务代码迁移...")
    migrate()
    print("\n迁移完成！")
    print("提示：此脚本采用‘复制’模式，确认无误后可手动删除 backend 目录。")