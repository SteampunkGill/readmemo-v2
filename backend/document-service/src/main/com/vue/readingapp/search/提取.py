import os

def extract_java_files_content(output_filename="all_java_files_content.txt"):
    """
    提取当前目录下所有 .java 文件的内容，并将它们合并到一个文本文件中。

    Args:
        output_filename (str): 保存所有 .java 文件内容的输出文件名。
    """
    current_directory = os.getcwd()
    print(f"正在扫描目录: {current_directory}")

    with open(output_filename, 'w', encoding='utf-8') as outfile:
        for filename in os.listdir(current_directory):
            # 检查是否是文件并且以 .java 结尾
            if os.path.isfile(filename) and filename.lower().endswith(".java"):
                print(f"正在处理 Java 文件: {filename}")
                try:
                    with open(filename, 'r', encoding='utf-8') as infile:
                        content = infile.read()
                        outfile.write(f"--- 开始文件: {filename} ---\n")
                        outfile.write(content)
                        outfile.write(f"\n--- 结束文件: {filename} ---\n\n")
                except Exception as e:
                    print(f"读取 Java 文件 {filename} 时出错: {e}")
                    outfile.write(f"--- 无法读取 Java 文件: {filename} (错误: {e}) ---\n\n")

    print(f"所有 Java 文件内容已成功提取并保存到: {output_filename}")

if __name__ == "__main__":
    extract_java_files_content()