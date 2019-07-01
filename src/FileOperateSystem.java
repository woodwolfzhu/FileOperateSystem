import java.util.*;


    public class FileOperateSystem {                    // 文件系统类
    String desinger;    // 设计者姓名
    String version;         // 版本号
    final int size;       //盘块大小，以byte 记，2的n次幂
    final int totalNumber;        //盘块总数量
    final int allowance;          //剩余空闲盘块
    final int root;               // 根目录位置
    final int fcbSize;             // fcb 表项大小
    String currentDirectory;   // 当前目录
    int currentLocation;    // 记录fat表当前的位置，一直往后寻找空闲盘块，然后循环
    FAT[] fat;
    Disk[] disk;
    Scanner sc;
    MyFile myfile;

    FileOperateSystem() {
        desinger = "朱志伟";
        version = "1.0";
        size = 4096;      // 盘块大小为4kb
        fcbSize = 16;       //
        totalNumber = 1024;
        root = 2;   // 数据块0、1不能使用
        allowance = totalNumber - 3; // 由上剩余数量为这么多
        currentDirectory = "/";    // 初始的当前目录就是root
        currentLocation = 2;
        fat = new FAT[totalNumber];
        disk = new Disk[totalNumber];
        initArray();
        fat[0].next = -2;
        fat[1].next = -2;  // 不使用fat表和数据块的第一二块;
        fat[2].next = -1; // 根目录结束于当前盘块

        disk[0].data = "F8FFFF0F".getBytes();
        disk[1].data = "FFFFFFFF".getBytes(); // 数据块一和二 不使用
        myfile = new MyFile(".", root);  // 在根目录中设置 . 访问项
        byte[] bt = myfile.getBytes();
        System.arraycopy(bt, 0, disk[currentLocation].data, 0, fcbSize);      // 将目录项放入相应数据块中
        myfile = new MyFile("/", root); // 为了更新路径，在目录项中加了这一块，只为了获得当前目录的名字
        bt = myfile.getBytes();
        System.arraycopy(bt, 0, disk[currentLocation].data, 2 * fcbSize, fcbSize);      // 将目录项放入相应数据块中
        // 统一放在第三个目录项的位置
        sc = new Scanner(System.in);
        operate();
    }

    void initArray() {
        int i = 0;
        for (i = 0; i < totalNumber; i++) {
            fat[i] = new FAT();
            disk[i] = new Disk(size);
        }
    }

    void operate() {     // 所有操作的入口
        System.out.print(currentDirectory + ">:");
        while (true) {
            String order = sc.nextLine();
            String[] subOrder = order.split(" ");
            if (subOrder.length < 2) {
                if (subOrder[0].equals("dir")) {
                    dir(order);
                    currentDirectory = resetPath();
                    System.out.print(currentDirectory + '\b' + ">:");
                    continue;
                } else if (subOrder[0].equals("exit")) {
                    return;
                } else {
                    System.out.println("命令输入有误，请重新输入！");
                    currentDirectory = resetPath();
                    System.out.print(currentDirectory + '\b' + ">:");
                    continue;
                }
            }
            switch (subOrder[0]) {
                case "md":
                    md(order);
                    break;   // 将分割后的命令传给相应的函数
                case "dir":
                    dir(order);
                    break;
                case "rd":
                    rd(order);
                    break;
                case "cd":
                    cd(order);
                    break;
                case "new":
                    newFile(order);
                    break;
                case "del":
                    rd(order);
                    break;
                case "edit":
                    editText(order);
                    break;
                case "type":
                    String text = type(order);
                    System.out.println(text);
                    break;
                case "copy":
                    copy(order);
                    break;
                case "attr":
                    attr(order);
                    break;
                case "":
                    break;
                default:
                    System.out.println("无法识别的命令！");
            }
            currentDirectory = resetPath();
            System.out.print(currentDirectory + '\b' + ">:");
        }
    }

    void md(String order) { // 新建目录
        String[] sub = order.split(" ");
        String[] subOrder = sub[1].split("/");
        if (subOrder.length == 1) {      // 在当前目录新建目录
            mdDir(subOrder);
        } else if (subOrder.length > 1) { // 根据相对路径或绝对路径建立目录
            mdDir(subOrder);
        } else {
            System.out.println("命令输入有误，请重新输入！");
        }
    }

    void mdDir(String[] subOrder) {      // 新建目录项
        String fileName = subOrder[subOrder.length - 1];

        int currentDisk = seekDir(subOrder);
        if (currentDisk == -4) {    // 目录不存在
            return;
        }
        currentLocation = currentDisk;

        // 判断当前目录中是否有同名文件
        String directory = printDir(currentLocation);
        if (directory.indexOf(fileName) != -1) {
            System.out.println("文件已存在！");
            return;
        }
        int newDisk1 = 0;
        newDisk1 = seekNullDisk();   // 为新目录项分配新的空白盘块
        if (newDisk1 == -3) {       // 所有存储空间已用完
            System.out.println("存储空间已用完！");
            return;
        }

        MyFile myFile1, myFile2;
        myFile1 = new MyFile(".", newDisk1);    // . 为本目录
        myFile2 = new MyFile("..", currentLocation); // .. 为父目录

        int i = seekNullData();        // 在当前数据块寻找空白表项
        if (i == -3) {            // 当前数据块已用完
            int newDisk2 = 0;
            newDisk2 = seekNullDisk();            // 寻找新的空白盘块
            if (newDisk2 == -3) {       // 所有存储空间已用完
                System.out.println("存储空间已用完！");
                return;
            }
            fat[currentLocation].next = newDisk2;      // 将新的数据块与上个用完的数据块链接
            currentLocation = newDisk2;                // 更新当前工作的数据块
        }
        // 当空白空间足够时，才能成功新建目录

        MyFile myFile = new MyFile(fileName, newDisk1);   // 建立目录项
        byte[] bt = myFile.getBytes();
        System.arraycopy(bt, 0, disk[currentLocation].data, 0 + i * fcbSize, fcbSize);      // 将目录项放入相应数据块中

        currentLocation = newDisk1;                 // 更新当前工作数据块为新目录的位置
        fat[currentLocation].next = -1;                     // 新建立的目录只占用一个数据块，用-1表示结束于本盘块

        i = 0;
        byte[] bt1 = myFile1.getBytes();
        System.arraycopy(bt1, 0, disk[currentLocation].data, 0 + i * fcbSize, fcbSize);      // 将目录项放入相应数据块中

        i = 1;
        byte[] bt2 = myFile2.getBytes();
        System.arraycopy(bt2, 0, disk[currentLocation].data, 0 + i * fcbSize, fcbSize);      // 将目录项放入相应数据块中

        i = 2;
        MyFile myFile3 = new MyFile(fileName, currentLocation);
        byte[] bt3 = myFile3.getBytes();
        System.arraycopy(bt3, 0, disk[currentLocation].data, 0 + i * fcbSize, fcbSize);      // 将目录项放入相应数据块中

        System.out.println("目录新建成功！");

        }

        void dir(String order) {     // 显示当前目录下的文件和子目录
            int i;
            int currentDisk1 = 0;
            int m = 0;
            String[] subOrder = order.split(" ");

            if (subOrder.length > 1) {     // 输入信息为 dir user/sa/sfds 这类指令
                String[] subOrder1 = subOrder[1].split("/");
                if (subOrder1.length == 0) {
                    currentDisk1 = root;
                    System.out.println(printDir(currentDisk1));
                    return;
                }
            currentDisk1 = seekDir(subOrder1);
            if (currentDisk1 == -4) { // 未找到相关项
                return;
            }

            for (i = 0; i * fcbSize < size; i++) {
                String fileName = new String(disk[currentDisk1].data, 0 + i * fcbSize, 9);

                fileName = removeNullChar(fileName); // 去掉末尾的空字符

                if (fileName.equals(subOrder1[subOrder1.length - 1])) {
                    currentDisk1 = (disk[currentDisk1].data[14 + i * fcbSize] << 8 & 0xff)
                            | (disk[currentDisk1].data[15 + i * fcbSize] & 0xff);

                    String extensionName = getClss(currentDisk1, i);

                    if (!extensionName.equals("dir")) {
                        System.out.println("目标文件不是目录");
                        return;
                    }
                    break;
                } else if (disk[currentDisk1].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所有要全部遍历
                    continue;
                }
            }
            if (i * fcbSize == size) {
                System.out.println("不存在" + subOrder1[subOrder1.length - 1] + " 目录");
                return;
            }
        } else {       // 输入的指令为 dir
            currentDisk1 = currentLocation;
        }
        System.out.println(printDir(currentDisk1)); // 获得当前目录下所有目录项
    }

    String printDir(int currentDisk) {  // 获取目录中的所有项
        int i;
        String directory = "";
        for (i = 1; i * fcbSize < size; i++) { // "." , "..",自己的名字，这三项都不用输出
            String fileName = new String(disk[currentDisk].data, 0 + i * fcbSize, 9);
            if (disk[currentDisk].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所以要全部遍历
                continue;
            }
            fileName = removeNullChar(fileName);
            if (i == 2 || fileName.equals("..")) {    // 之所以不直接从第四项开始而是用判断来控制，是因为root目录下的第二项可以存其他目录
                // 可以将 disk[2].data[fcbSize] 设置为一个假目录项，那样就可以直接从第四项开始遍历了，以空间换时间
                continue;
            }

            directory = directory + fileName + "   ";
        }
        return directory;
    }

    void rd(String order) {
        int i;
        int currentDisk1 = 0;
        int m = 0;
        String[] subOrder = order.split(" ");
        String[] subOrder1 = subOrder[1].split("/");
        currentDisk1 = seekDir(subOrder1);
        if (currentDisk1 == -4) {     // 未找到相关项
            return;
        }
        for (i = 0; i * fcbSize < size; i++) {
            String fileName = new String(disk[currentDisk1].data, 0 + i * fcbSize, 9);

            fileName = removeNullChar(fileName);    // 去掉末尾的空白字符

            if (fileName.equals(subOrder1[subOrder1.length - 1])) {

                boolean attribute = (disk[currentDisk1].data[12 + i * fcbSize] == 0x00) ? false : true;
                if (!attribute) {
//                    currentDisk1 = (disk[currentDisk1].data[14 + i * fcbSize] << 8 & 0xff)
//                            | (disk[currentDisk1].data[15 + i * fcbSize] & 0xff);
                    delFileorDir(i, currentDisk1);
                } else {
                    System.out.println("文件只读，不可删除！");
                }

                // 清空
                break;
            } else if (disk[currentDisk1].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所有要全部遍历
                continue;
            }
        }
        if (i * fcbSize == size) {
            System.out.println("不存在" + subOrder1[subOrder1.length - 1] + " 目录");
            return;
        }
    }

    void delFileorDir(int i, int currentDisk1) { // 删除文件或目录项
        int desDisk = (disk[currentDisk1].data[14 + i * fcbSize] << 8 & 0xff)
                + (disk[currentDisk1].data[15 + i * fcbSize] & 0xff);
        if(desDisk == currentLocation){
            System.out.println("正在此目录中，无法删除，请换到其他目录下，在尝试！");
            return;
        }
        String attribute = new String(disk[currentDisk1].data, 9 + i * fcbSize, 3);
        attribute = removeNullChar(attribute);
        if (attribute.equals("dir")) {
            if (!ifEmpty(desDisk)) {
                System.out.println("无法删除非空目录项！");
                return;
            }
        }
        int x = desDisk;
        int temp;
        byte[] nullByte = new byte[size];
        while (fat[x].next != -1) {       // 将fat 表中相关内容删除
            temp = fat[x].next;
            fat[x].next = 0;
            disk[x].data =nullByte; // 将相应数据块，避免不可知的错误，例如在曾经使用过的数据块上新建目录时
            x = temp;
        }
        disk[x].data =nullByte;
        fat[x].next = 0;

        temp = i * fcbSize;
        for (x = 0; x < fcbSize; x++) {         // 删除相应的目录项
            disk[currentDisk1].data[temp + x] = '\0';
        }
        System.out.println("删除成功！");
    }

    String getClss(int currentDisk, int i) { // 获得目标文件的类型
        String extensionName = new String(disk[currentDisk].data, 9 + i * fcbSize, 3);
        extensionName = removeNullChar(extensionName);  // 去掉末尾的空白字符
        return extensionName;
    }

    boolean ifEmpty(int currentLocation) {  // 判断目录是否为空
        int i = 3;
        for (i = 3; i * fcbSize < size; i++) {
            String name = new String(disk[currentLocation].data, i * fcbSize, 9);
            name = removeNullChar(name);
            if (!name.equals("")) {
                return false;
            }
        }
        return true;
    }

    void cd(String order) {
        int i;
        int currentDisk1 = 0;
        int m = 0;
        String[] subOrder = order.split(" ");
        String[] subOrder1 = subOrder[1].split("/");

        if (subOrder1.length < 1) {       // 直接切换到根目录
            currentLocation = root;
            return;
        }

        currentDisk1 = seekDir(subOrder1);
        if (currentDisk1 == -4) {     // 未找到相关项
            return;
        }

        for (i = 0; i * fcbSize < size; i++) {
            if (disk[currentDisk1].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所有要全部遍历
                continue;
            }

            String fileName = new String(disk[currentDisk1].data, 0 + i * fcbSize, 9);

            fileName = removeNullChar(fileName);    // 去掉末尾的空白字符


            if (fileName.equals(subOrder1[subOrder1.length - 1])) {

                String extensionName = getClss(currentDisk1, i);
                if (!extensionName.equals("dir")) {
                    System.out.println("目标文件不是目录，命令无法执行！");
                    return;
                }
                currentLocation = (disk[currentDisk1].data[14 + i * fcbSize] << 8 & 0xff)
                        | (disk[currentDisk1].data[15 + i * fcbSize] & 0xff);
                break;
            }
        }
        if (i * fcbSize == size) {
            System.out.println("不存在" + subOrder1[subOrder1.length - 1] + " 目录");
            return;
        }
    }

    void newFile(String order) {
        String[] subOrder = order.split(" ");
        String[] subOrder1 = subOrder[1].split("/");
        if (subOrder1.length == 1) {      // 在当前目录新建目录
            nf(subOrder1);
        } else if (subOrder1.length > 1) {
            nf(subOrder1);
        } else {
            System.out.println("命令输入有误，请重新输入！");
            return;
        }

    }

    void nf(String[] order) {       // 采用第一种实现方式，先建立文件，之后在编辑文件

        String fileNme = order[order.length - 1];
        int newDisk1 = 0;
        newDisk1 = seekNullDisk();   // 为文件分配新的空白盘块
        if (newDisk1 == -3) {       // 所有存储空间已用完
            System.out.println("存储空间已用完！");
            return;
        }

        int currentDisk = seekDir(order);// 下面的操作会对currentLocation做出不正确的改变，所以这里先把他们存下来
        if (currentDisk == -4) {
            return;
        }
        currentLocation = currentDisk;

        int i = seekNullData();        // 在当前数据块寻找空白表项
        if (i == -3) {            // 当前数据块已用完
            int newDisk2 = 0;
            newDisk2 = seekNullDisk();            // 寻找新的空白盘块
            if (newDisk2 == -3) {       // 所有存储空间已用完
                System.out.println("存储空间已用完！");
                return;
            }

            fat[currentLocation].next = newDisk2;      // 将新的数据块与上个用完的数据块链接
            currentLocation = newDisk2;                // 更新当前工作的数据块
        }
        // 当空白空间足够时，才能成功新建文件

        String[] name = fileNme.split("\\.");
        if (name.length != 2) {
            System.out.println("文件格式不正确！");
            return;
        }
        MyFile myFile = new MyFile(fileNme, name[1], newDisk1);   // 建立目录项
        byte[] bt = myFile.getBytes();
        System.arraycopy(bt, 0, disk[currentLocation].data, 0 + i * fcbSize, fcbSize);      // 将目录项放入相应数据块中

        fat[newDisk1].next = -1;                     // 新建立的文件只占用一个数据块，用-1表示结束于本盘块
        currentLocation = currentDisk;                 // 更新当前工作数据块为之前目录所在位置，新建文件不应当更改当前位置

        System.out.println("文件新建成功！");
    }

    void editText(String order) {
        int i;
        int currentDisk1 = 0;
        int m = 0;
        String[] subOrder = order.split(" ");
        String[] subOrder1 = subOrder[1].split("/");
        String[] name = subOrder1[subOrder1.length - 1].split("\\.");
        if (name.length < 1) {
            System.out.println("输入的文件格式不正确，无法编辑！");
            return;
        }
        currentDisk1 = seekDir(subOrder1);
        if (currentDisk1 == -4) {     // 未找到相关项
            return;
        }
        for (i = 0; i * fcbSize < size; i++) {
            String fileName = new String(disk[currentDisk1].data, 0 + i * fcbSize, 9);

            fileName = removeNullChar(fileName);    // 去掉末尾的空白字符

            if (fileName.equals(subOrder1[subOrder1.length - 1])) {

                // 对文件属性进行判断，看是否只读
                boolean attribute;
                attribute = (disk[currentDisk1].data[12 + i * fcbSize] == 0x00) ? false : true;
                String extensionName = getClss(currentDisk1, i);

                if (extensionName.equals("dir")) {
                    System.out.println("目标文件不是文本文件！");
                    return;
                }
                if (!attribute) {
                    currentDisk1 = (disk[currentDisk1].data[14 + i * fcbSize] << 8 & 0xff)
                            | (disk[currentDisk1].data[15 + i * fcbSize] & 0xff);
                    text(currentDisk1);
                } else {
                    System.out.println("文件只读，不可编辑！");
                }
                break;

            } else if (disk[currentDisk1].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所有要全部遍历
                continue;
            }
        }
        if (i * fcbSize == size) {
            System.out.println("不存在" + subOrder1[subOrder1.length - 1] + " 文件");
            return;
        }
    }

    void text(int currentDisk) {     // 输入内容并将内容写入数据块
        // 先将数据块清空

        int x = fat[currentDisk].next;
        if (x != -1) {
            int temp;
            while (fat[x].next != -1) {       // 将fat 表中相关内容删除
                temp = fat[x].next;
                fat[x].next = 0;
                x = temp;
            }
            fat[x].next = 0;
        }

        // 读入内容
        String text = "";
        System.out.println("请输入内容(输入#号结束)：");
        while (true) {
            text = text + sc.nextLine();
            if (text.charAt(text.length() - 1) == '#') {
                break;
            }
        }

        // 申请盘块
        byte[] tb = text.getBytes();
        int num = tb.length / size;
        int[] newDisk1 = new int[num];
        int i = 0;
        for (i = 0; i < num; i++) {
            newDisk1[i] = seekNullDisk();
            fat[newDisk1[i]].next = -5; // 只占位，后面如果空间不足的话还要删除
            if (newDisk1[i] == -3) {
                System.out.println("存储空间不足,编辑失败");
                int m = 0;
                for (m = 0; m <= i; m++) {      // 将刚才申请的空间释放
                    fat[newDisk1[i]].next = 0;
                }
                return;
            }
        }

        // 数据存入盘块
        i = 0;
        while (tb.length - size * (i + 1) > 0) {
            System.arraycopy(tb, i * size, disk[currentDisk].data, 0, size);
            fat[currentDisk].next = newDisk1[i];
            currentDisk = newDisk1[i];
            i++;
        }
        System.arraycopy(tb, i * size, disk[currentDisk].data, 0, tb.length - i * size);
        fat[currentDisk].next = -1;
        System.out.println("数据保存成功！");

    }

    String type(String order) {    // 查看文件
        int i;
        int currentDisk1 = 0;
        String text = new String();
        String[] subOrder = order.split(" ");
        String[] subOrder1 = subOrder[1].split("/");
        currentDisk1 = seekDir(subOrder1);
        if (currentDisk1 == -4) {     // 未找到相关项
            System.out.println("文件不存在");
            return " ";
        }
        for (i = 0; i * fcbSize < size; i++) {
            String fileName = new String(disk[currentDisk1].data, 0 + i * fcbSize, 9);

            fileName = removeNullChar(fileName);    // 去掉末尾的空白字符

            if (fileName.equals(subOrder1[subOrder1.length - 1])) {
                // 判断扩展名是否为dir，不是才可查看
                String extensionName = new String(disk[currentDisk1].data, 9 + i * fcbSize, 3);

                extensionName = removeNullChar(extensionName);  // 去掉末尾的空白字符

                if (extensionName.equals("dir")) {
                    System.out.println("目标文件不是文本文件");
                    return " ";
                }
                text = openFile(currentDisk1, i);
                // 打开数据块
                break;
            } else if (disk[currentDisk1].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所有要全部遍历
                continue;
            }
        }
        if (i * fcbSize == size) {
            System.out.println("不存在" + subOrder1[subOrder1.length - 1] + " 文本文件");
            return " ";
        }
        return text;
    }

    String openFile(int currentDisk, int i) {
        // 获得首块号
        int currentDisk1 = (disk[currentDisk].data[14 + i * fcbSize] << 8 & 0xff)
                | (disk[currentDisk].data[15 + i * fcbSize] & 0xff);
        // 获得总块数，建立合适的字节数组
        int num = 1;
        int location = currentDisk1;
        while (fat[location].next != -1) {
            num++;
            location = fat[location].next;
        }
        byte[] tb = new byte[num * size];

        // 将数据块内容读入字节数组
        location = currentDisk1;
        num = 0;
        while (fat[location].next != -1) {
            System.arraycopy(disk[location].data, 0, tb, num * size, size);
            location = fat[location].next;
        }
        System.arraycopy(disk[location].data, 0, tb, num * size, size);
        String text = new String(tb);
        return text;
    }

    void copy(String order) {
        String[] subOrder = order.split(" ");
        if(subOrder.length == 3) {
            String order1 = "new " + subOrder[2];
            newFile(order1);
            String order2 = "type " + subOrder[1];
            String srcText = type(order2);
            String order3 = "edit " + subOrder[2];
            editText(order3, srcText);
        }
        else{
            System.out.println("命令输入错误！");
        }
    }

    void attr(String order) {
        int i;
        int currentDisk1 = 0;
        int m = 0;
        String[] subOrder = order.split(" ");
        String[] subOrder1 = subOrder[1].split("/");
        String[] name = subOrder1[subOrder1.length - 1].split("\\.");
        if (name.length < 1) {
            System.out.println("输入的文件格式不正确，无法编辑！");
            return;
        }
        currentDisk1 = seekDir(subOrder1);
        if (currentDisk1 == -4) {     // 未找到相关项
            return;
        }

        for (i = 0; i * fcbSize < size; i++) {
            String fileName = new String(disk[currentDisk1].data, 0 + i * fcbSize, 9);

            fileName = removeNullChar(fileName);    // 去掉末尾的空白字符

            if (fileName.equals(subOrder1[subOrder1.length - 1])) { // 找到目标项
                switch (subOrder[2]) {        // 根据命令，对属性进行设置
                    case "-r":
                        disk[currentDisk1].data[12 + i * fcbSize] = 0x00;
                        System.out.println("属性修改成功！");
                        break;
                    case "+r":
                        disk[currentDisk1].data[12 + i * fcbSize] = 0x01;   // 只读，不可编辑
                        System.out.println("属性修改成功！");
                        break;
                    default:
                        System.out.println("属性输入有误");
                }
                break;
            } else if (disk[currentDisk1].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所有要全部遍历
                continue;
            }
        }
        if (i * fcbSize == size) {
            System.out.println("不存在" + subOrder1[subOrder1.length - 1] + " 文件");
            return;
        }
    }

    void editText(String order, String text) {
        int i;
        int currentDisk1 = 0;
        int m = 0;
        String[] subOrder = order.split(" ");

        String[] subOrder1 = subOrder[1].split("/");
        String[] name = subOrder1[subOrder1.length - 1].split("\\.");
        if (name.length < 1) {
            System.out.println("输入的文件格式不正确，无法编辑！");
            return;
        }

        currentDisk1 = seekDir(subOrder1);
        if (currentDisk1 == -4) {     // 未找到相关项
            return;
        }
        for (i = 0; i * fcbSize < size; i++) {
            String fileName = new String(disk[currentDisk1].data, 0 + i * fcbSize, 9);
            fileName = removeNullChar(fileName);    // 去掉末尾的空白字符
            if (fileName.equals(subOrder1[subOrder1.length - 1])) {
                currentDisk1 = (disk[currentDisk1].data[14 + i * fcbSize] << 8 & 0xff)
                        | (disk[currentDisk1].data[15 + i * fcbSize] & 0xff);
                text(currentDisk1, text);
                break;
            } else if (disk[currentDisk1].data[0 + i * fcbSize] == 0) {     // 为防止删除处于中间的文件或目录导致提前退出，所有要全部遍历
                continue;
            }
        }
        if (i * fcbSize == size) {
            System.out.println("不存在" + subOrder1[subOrder1.length - 1] + " 文件");
            return;
        }
    }

    void text(int currentDisk, String text) {     // 输入内容并将内容写入数据块
        // 先将数据块清空
        int x = fat[currentDisk].next;

        // 申请盘块
        byte[] tb = text.getBytes();
        int num = tb.length / size;
        int[] newDisk1 = new int[num];
        int i = 0;
        for (i = 0; i < num; i++) {
            newDisk1[i] = seekNullDisk();
            fat[newDisk1[i]].next = -5; // 只占位，后面如果空间不足的话还要删除
            if (newDisk1[i] == -3) {
                System.out.println("存储空间不足,编辑失败");
                int m = 0;
                for (m = 0; m <= i; m++) {      // 将刚才申请的空间释放
                    fat[newDisk1[i]].next = 0;
                }
                return;
            }
        }

        // 数据存入盘块
        i = 0;
        while (tb.length - size * (i + 1) > 0) {
            System.arraycopy(tb, i * size, disk[currentDisk].data, 0, size);
            fat[currentDisk].next = newDisk1[i];
            currentDisk = newDisk1[i];
            i++;
        }
        System.arraycopy(tb, i * size, disk[currentDisk].data, 0, tb.length - i * size);
        fat[currentDisk].next = -1;
        System.out.println("数据保存成功！");
    }

    int seekNullData() {        // 在当前盘块中寻找空白的表项
        int i;
        for (i = 0; i + fcbSize < size; i++) {
            if (disk[currentLocation].data[0 + i * fcbSize] == 0) {
                return i;
            }
        }
        return -3;
    }

    int seekNullDisk() {     // 寻找新的空白盘块
        int i = currentLocation;
        int x = 0;
        for (x = 0; x < totalNumber; x++) {
            if (fat[(i + x) % totalNumber].next == 0) {
                return (i + x) % totalNumber;
            }
        }
        return -3;      // 表示所有空间已用完
    }

    int seekDir(String[] order) {      // 根据路径寻找相应的目录项所在的数据块
        // 为了方便多个功能调用，只找到倒数第二个位置,例如：../data/li，就只找到data的位置
        int diskLocation = 0;
        int i = 0;
        int x = 0;
        if (order[0].equals("")) {
            diskLocation = root;
            x = 1;
        } else {
            diskLocation = currentLocation;
            x = 0;
        }

        for (x = x; x < order.length - 1; x++) {
            for (i = 0; i * fcbSize < size; i++) {      // 为了保证不漏掉，要全部遍历一次
                if (disk[diskLocation].data[0 + i * fcbSize] == 0) {
                    continue;
                }
                // 获得正确的文件名
                String fileName = new String(disk[diskLocation].data, i * fcbSize, 9);
                fileName = removeNullChar(fileName);    // 去掉末尾的空白字符
                // 判断目录项中文件名是否相同
                if (fileName.equals(order[x])) {
                    diskLocation = (disk[diskLocation].data[14 + i * fcbSize] << 8 & 0xff)
                            | (disk[diskLocation].data[15 + i * fcbSize] & 0xff);
                    break;
                }
            }
            if (i * fcbSize == size) {
                System.out.println("不存在" + order[x] + " 目录");
                return -4;          //  目录不存的标记为 -4
            }
        }
        if (i * fcbSize == size) {
            System.out.println("不存在" + order[x] + " 目录");
            return -4;          //  目录不存的标记为 -4
        }
        return diskLocation;
    }

    String removeNullChar(String name) {     // 将byte数组转成String后，去掉String 中的空字符
        // i 为
        int m;
        for (m = name.length() - 1; m >= 0; m--) {
            if (name.charAt(m) != '\u0000') {
                break;
            }
        }
        name = name.substring(0, m + 1);
        return name;
    }

    String resetPath() {
        // 获得md cd 命令后的目录.
        int currentDisk = currentLocation;
        int n;
        String path = "";
        String name;
        while (true) {
            name = new String(disk[currentDisk].data, 2 * fcbSize, 9); // 获得当前所在目录的名字
            name = removeNullChar(name);  // 去掉末尾空白字符
            if (!name.equals("/")) {       // 判断是否到了根目录
                path = name + "/" + path;   // 添加名字到路径
                currentDisk = (disk[currentDisk].data[14 + fcbSize] >> 8 & 0xff)
                        + (disk[currentDisk].data[15 + fcbSize] & 0xff);     // 获得父目录的首块号
            } else {
                path = name + path;   // 添加名字到路径,根目录本来就是"/" ，再加"/"就重复了
                break;
            }
        }
        if (path.equals("/")) {   // 如果当前目录就是根目录，那么path="/",在上面输出路径时会退格，那么输出的内容就不合适
            // 所以这里要加一个"/"
            path = path + "/";
        }
        return path;
    }

    public static void main(String[] args) {
        FileOperateSystem a = new FileOperateSystem();
        System.out.println("设计者: " + a.desinger);
        System.exit(1);
    }
}

class FAT {
    // -2 表示不能存放文件的，第一二块
    // -1 文件的结束标记
    // 0 空白数据块
    // -3 表示数据块已满
    // 其他正整数代表同一文件的相邻数据块
    int next;

    FAT() {
        next = 0;
    }

}

class Disk {            // 数据块
    byte[] data;

    Disk(int size) {
        data = new byte[size];
    }
}

class MyFile {        // 文件对象：文本文件、目录 每项16字节
    String fileName;    // 文件名  9字节
    String extensionName;   // 扩展名 3字节
    boolean read_only;      // 是否只读 1字节
    boolean isDirectory;    // 是否目录 1字节
    int firstDisk;  //首块号 2字节

    MyFile(String fileName, String extensionName, int firstDisk) {  // 文本文件
        this.fileName = fileName;
        this.extensionName = extensionName;
        this.read_only = false;
        this.isDirectory = false;
        this.firstDisk = firstDisk;
    }

    MyFile(String fileName, int firstDisk) {   // 目录项
        this.fileName = fileName;
        this.extensionName = "dir";
        this.read_only = false;
        this.isDirectory = true;
        this.firstDisk = firstDisk;
    }

    byte[] getBytes() {                     // 将文件属性的相关信息转化为byte；
        byte[] bt = fileName.getBytes();
        int i;
        byte[] bt1 = new byte[9];
        for (i = 0; i < 9; i++) {           // 确保文件名占用9个字节
            if (i < bt.length) {
                bt1[i] = bt[i];
                continue;
            }
            bt1[i] = '\0';
        }

        bt = extensionName.getBytes();
        byte[] bt2 = new byte[3];
        for (i = 0; i < 3; i++) {           // 确保扩展名名占用3个字节
            if (i < bt.length) {
                bt2[i] = bt[i];
                continue;
            }
            bt2[i] = '\0';
        }

        byte[] bt3 = new byte[1];
        if (read_only) {
            bt3[0] = 0x01;
        } else {
            bt3[0] = 0x00;
        }

        byte[] bt4 = new byte[1];
        if (isDirectory) {
            bt4[0] = 0x01;
        } else {
            bt4[0] = 0x00;
        }
        byte[] bt5 = new byte[2];
        bt5[0] = (byte) ((firstDisk >> 8) & 0xff);
        bt5[1] = (byte) (firstDisk & 0xff);

        byte[] both = concat(bt1, bt2, bt3, bt4, bt5);
        return both;
    }

    byte[] concat(byte[] bt1, byte[] bt2, byte[] bt3, byte[] bt4, byte[] bt5) {        // 将5 个byte数组链接成一个
        byte[] both = new byte[bt1.length + bt2.length + bt3.length + bt4.length + bt5.length];
        System.arraycopy(bt1, 0, both, 0, bt1.length);
        System.arraycopy(bt2, 0, both, bt1.length, bt2.length);
        System.arraycopy(bt3, 0, both, bt1.length + bt2.length, bt3.length);
        System.arraycopy(bt4, 0, both, bt1.length + bt2.length + bt3.length, bt4.length);
        System.arraycopy(bt5, 0, both, bt1.length + bt2.length + bt3.length + bt4.length, bt5.length);
        return both;
    }
}