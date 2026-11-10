param([switch]$Check, [switch]$Start, [switch]$NoBrowser, [string]$ProjectRoot=(Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference='Stop'
trap {
    if(-not $Check -and -not $Start){
        Add-Type -AssemblyName System.Windows.Forms
        [void][Windows.Forms.MessageBox]::Show('启动器无法打开：'+$_.Exception.Message,'Nexus Office')
    } else {
        [Console]::Error.WriteLine($_.Exception.Message)
    }
    exit 1
}
$core=Join-Path $PSScriptRoot 'demo-launcher-core.ps1'
. $core
if($Check){$result=Get-DemoEnvironment $ProjectRoot; $result | ConvertTo-Json -Depth 6; if(-not $result.Allowed){exit 1}; exit 0}
if($Start){Start-DemoApplication $ProjectRoot -NoBrowser:$NoBrowser | ConvertTo-Json; exit 0}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[Windows.Forms.Application]::EnableVisualStyles()
$mutex=New-Object Threading.Mutex($false,'Local\NexusOfficeDemoLauncher')
if(-not $mutex.WaitOne(0)){
    [void][Windows.Forms.MessageBox]::Show('启动器已经打开，请查看任务栏。','Nexus Office')
    $mutex.Dispose(); exit
}

$form=New-Object Windows.Forms.Form
$form.Text='Nexus Office | 本机 Demo 启动器'
$form.Size=New-Object Drawing.Size(1120,790)
$form.MinimumSize=New-Object Drawing.Size(940,650)
$form.StartPosition='CenterScreen'
$form.Font=New-Object Drawing.Font('Microsoft YaHei UI',10)
$form.BackColor=[Drawing.Color]::FromArgb(246,248,250)

$layout=New-Object Windows.Forms.TableLayoutPanel
$layout.Dock='Fill'; $layout.Padding=New-Object Windows.Forms.Padding(20)
$layout.ColumnCount=1; $layout.RowCount=5
foreach($height in @(52,42,48)){[void]$layout.RowStyles.Add((New-Object Windows.Forms.RowStyle('Absolute',$height)))}
[void]$layout.RowStyles.Add((New-Object Windows.Forms.RowStyle('Percent',100)))
[void]$layout.RowStyles.Add((New-Object Windows.Forms.RowStyle('Absolute',30)))
$form.Controls.Add($layout)
$title=New-Object Windows.Forms.Label
$title.Text='Nexus Office   本机演示环境'; $title.Font=New-Object Drawing.Font('Microsoft YaHei UI',19,[Drawing.FontStyle]::Bold); $title.Dock='Fill'
$layout.Controls.Add($title,0,0)
$status=New-Object Windows.Forms.Label
$status.Text='正在检测本机环境…'; $status.Dock='Fill'; $status.ForeColor=[Drawing.Color]::FromArgb(40,90,180)
$layout.Controls.Add($status,0,1)
$actions=New-Object Windows.Forms.FlowLayoutPanel
$actions.Dock='Fill'; $actions.WrapContents=$false
$layout.Controls.Add($actions,0,2)
function New-Action([string]$Text,[int]$Width=120){
    $button=New-Object Windows.Forms.Button
    $button.Text=$Text; $button.Size=New-Object Drawing.Size($Width,34); $button.Margin=New-Object Windows.Forms.Padding(0,0,10,0)
    $button.FlatStyle='Flat'; $button.BackColor=[Drawing.Color]::White
    [void]$actions.Controls.Add($button); return $button
}
$checkButton=New-Action '重新检测'
$startButton=New-Action '一键启动并打开网页' 200
$startButton.Enabled=$false; $startButton.BackColor=[Drawing.Color]::FromArgb(34,106,221); $startButton.ForeColor=[Drawing.Color]::White
$webButton=New-Action '打开项目网页' 140; $webButton.Enabled=$false
$folderButton=New-Action '打开源码目录' 140
$logsButton=New-Action '打开日志目录' 140

$tabs=New-Object Windows.Forms.TabControl; $tabs.Dock='Fill'
$layout.Controls.Add($tabs,0,3)
$environmentTab=New-Object Windows.Forms.TabPage; $environmentTab.Text='环境检查'
$sourceTab=New-Object Windows.Forms.TabPage; $sourceTab.Text='查看源码'
$logTab=New-Object Windows.Forms.TabPage; $logTab.Text='启动进度'
$tabs.TabPages.AddRange(@($environmentTab,$sourceTab,$logTab))

$envLayout=New-Object Windows.Forms.TableLayoutPanel
$envLayout.Dock='Fill'; $envLayout.ColumnCount=1; $envLayout.RowCount=2
[void]$envLayout.RowStyles.Add((New-Object Windows.Forms.RowStyle('Percent',70)))
[void]$envLayout.RowStyles.Add((New-Object Windows.Forms.RowStyle('Percent',30)))
$environmentTab.Controls.Add($envLayout)
$grid=New-Object Windows.Forms.DataGridView
$grid.Dock='Fill'; $grid.ReadOnly=$true; $grid.AllowUserToAddRows=$false; $grid.AllowUserToDeleteRows=$false
$grid.RowHeadersVisible=$false; $grid.SelectionMode='FullRowSelect'; $grid.MultiSelect=$false
$grid.AutoSizeColumnsMode='Fill'; $grid.BackgroundColor=[Drawing.Color]::White; $grid.BorderStyle='None'
[void]$grid.Columns.Add('name','检查项'); [void]$grid.Columns.Add('state','状态'); [void]$grid.Columns.Add('detail','检测结果')
$grid.Columns[0].FillWeight=27; $grid.Columns[1].FillWeight=15; $grid.Columns[2].FillWeight=58
$envLayout.Controls.Add($grid,0,0)
$help=New-Object Windows.Forms.RichTextBox
$help.Dock='Fill'; $help.ReadOnly=$true; $help.BackColor=[Drawing.Color]::White; $help.BorderStyle='FixedSingle'; $help.DetectUrls=$true
$envLayout.Controls.Add($help,0,1)
$help.Add_LinkClicked({param($sender,$event) if($event.LinkText -match '^https://'){Start-Process $event.LinkText}})

$sourceLayout=New-Object Windows.Forms.TableLayoutPanel
$sourceLayout.Dock='Fill'; $sourceLayout.ColumnCount=1; $sourceLayout.RowCount=2
[void]$sourceLayout.RowStyles.Add((New-Object Windows.Forms.RowStyle('Absolute',40)))
[void]$sourceLayout.RowStyles.Add((New-Object Windows.Forms.RowStyle('Percent',100)))
$sourceTab.Controls.Add($sourceLayout)
$sources=@(
    'tools/demo-launcher.ps1','tools/demo-launcher-core.ps1','start-local.ps1','stop-local.ps1',
    'collab-office-web/src/main.ts','collab-office-web/src/App.vue','collab-office-web/src/views/roles/SystemAdminPage.vue',
    'collab-office-service/user-service/src/main/java/com/collaboffice/user/UserServiceApplication.java','LOCAL-RUN.md'
)
$sourceSelect=New-Object Windows.Forms.ComboBox; $sourceSelect.DropDownStyle='DropDownList'; $sourceSelect.Dock='Fill'
$sourceSelect.Items.AddRange($sources)
$sourceLayout.Controls.Add($sourceSelect,0,0)
$source=New-Object Windows.Forms.RichTextBox
$source.Dock='Fill'; $source.ReadOnly=$true; $source.WordWrap=$false; $source.BackColor=[Drawing.Color]::White
$source.Font=New-Object Drawing.Font('Consolas',10); $source.DetectUrls=$false
$sourceLayout.Controls.Add($source,0,1)
$sourceSelect.Add_SelectedIndexChanged({
    $file=Join-Path $ProjectRoot $sources[$sourceSelect.SelectedIndex]
    $source.Text=if(Test-Path -LiteralPath $file){Get-Content -LiteralPath $file -Encoding UTF8 -Raw}else{'该源码文件不存在，请恢复完整仓库。'}
})
$sourceSelect.SelectedIndex=0

$log=New-Object Windows.Forms.RichTextBox
$log.Dock='Fill'; $log.ReadOnly=$true; $log.WordWrap=$false; $log.BackColor=[Drawing.Color]::White
$log.Font=New-Object Drawing.Font('Consolas',10)
$logTab.Controls.Add($log)
$footer=New-Object Windows.Forms.Label
$footer.Dock='Fill'; $footer.Text='项目：'+$ProjectRoot; $footer.AutoEllipsis=$true; $footer.ForeColor=[Drawing.Color]::DimGray
$layout.Controls.Add($footer,0,4)

$script:job=$null; $script:lastCheck=$null; $script:mode=''; $script:started=Get-Date
function Start-LauncherJob([string]$Mode){
    if($script:job){return}
    $script:mode=$Mode; $script:started=Get-Date
    $checkButton.Enabled=$false; $startButton.Enabled=$false; $webButton.Enabled=$false
    $script:job=[PowerShell]::Create()
    $code=if($Mode -eq 'check'){'param($core,$root) . $core; Get-DemoEnvironment $root'}else{'param($core,$root) . $core; Start-DemoApplication $root'}
    [void]$script:job.AddScript($code).AddArgument($core).AddArgument($ProjectRoot)
    $script:handle=$script:job.BeginInvoke()
    $status.Text=if($Mode -eq 'check'){'正在检测本机环境…'}else{'正在启动，请保持窗口打开；就绪后自动打开网页。'}
    $status.ForeColor=[Drawing.Color]::FromArgb(40,90,180)
    if($Mode -eq 'start'){$tabs.SelectedTab=$logTab}
}
function Show-SelectedCheck {
    if(-not $script:lastCheck -or $grid.SelectedRows.Count -eq 0){return}
    $item=$grid.SelectedRows[0].Tag
    if(-not $item){return}
    $help.Text=$item.Name+"`r`n"+$item.Detail+"`r`n`r`n"+$item.Help
    if($item.Url){$help.AppendText("`r`n下载 / 官方说明："+$item.Url)}
}
$grid.Add_SelectionChanged({Show-SelectedCheck})
$timer=New-Object Windows.Forms.Timer; $timer.Interval=300
$timer.Add_Tick({
    if(-not $script:job){return}
    if($script:mode -eq 'start'){
        $elapsed=[int]((Get-Date)-$script:started).TotalSeconds
        $status.Text="正在启动（已用 $elapsed 秒）；前端与后端就绪后自动打开网页。"
        $logFile=Join-Path $ProjectRoot 'tools/native-runtime/startup.log'
        if(Test-Path -LiteralPath $logFile){try{$tail=(Get-Content -LiteralPath $logFile -Tail 35 -Encoding UTF8)-join "`r`n"; if($tail -ne $log.Text){$log.Text=$tail; $log.SelectionStart=$log.TextLength; $log.ScrollToCaret()}}catch{}}
    }
    if(-not $script:handle.IsCompleted){return}
    try {
        $result=@($script:job.EndInvoke($script:handle))
        if($script:job.HadErrors){throw $script:job.Streams.Error[0].Exception}
        if($script:mode -eq 'check'){
            $script:lastCheck=$result[-1]
            $grid.Rows.Clear()
            foreach($item in $script:lastCheck.Rows){
                $index=$grid.Rows.Add($item.Name,$item.State,$item.Detail)
                $grid.Rows[$index].Tag=$item
                if(-not $item.Passed){$grid.Rows[$index].DefaultCellStyle.ForeColor=if($item.Required){[Drawing.Color]::Firebrick}else{[Drawing.Color]::DarkGoldenrod}}
            }
            $startButton.Enabled=$script:lastCheck.Allowed
            $status.Text=if($script:lastCheck.Allowed){'允许启动：必需环境检查通过。'}else{'暂不允许启动：'+$script:lastCheck.BlockedCount+'项需要补齐或处理；选中红色行查看下载/处理方法。'}
            $status.ForeColor=if($script:lastCheck.Allowed){[Drawing.Color]::ForestGreen}else{[Drawing.Color]::Firebrick}
            Show-SelectedCheck
        }else{
            $status.Text='启动成功：前端、后端和网关已就绪，网页已打开。'
            $status.ForeColor=[Drawing.Color]::ForestGreen
            $webButton.Enabled=$true
        }
    } catch {
        $status.Text='未完成：'+$_.Exception.Message
        $status.ForeColor=[Drawing.Color]::Firebrick
        $help.Text=$status.Text+"`r`n请查看启动进度或日志目录，修复后重新检测。"
        $tabs.SelectedTab=$environmentTab
    } finally {
        $script:job.Dispose(); $script:job=$null; $checkButton.Enabled=$true
    }
})
$checkButton.Add_Click({Start-LauncherJob 'check'})
$startButton.Add_Click({Start-LauncherJob 'start'})
$webButton.Add_Click({Start-Process 'http://127.0.0.1:3000/'})
$folderButton.Add_Click({Start-Process explorer.exe -ArgumentList ('"'+$ProjectRoot+'"')})
$logsButton.Add_Click({$path=Join-Path $ProjectRoot 'tools/native-runtime'; if(Test-Path -LiteralPath $path){Start-Process explorer.exe -ArgumentList ('"'+$path+'"')}})
$form.Add_Shown({$timer.Start(); Start-LauncherJob 'check'})
try { [void]$form.ShowDialog() }
finally {
    $timer.Stop(); $timer.Dispose()
    if($script:job){$script:job.Stop(); $script:job.Dispose()}
    $mutex.ReleaseMutex(); $mutex.Dispose(); $form.Dispose()
}
