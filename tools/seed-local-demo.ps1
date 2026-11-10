param([switch]$PopulateWorkspace)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$accountFile = "$root/tools/native-runtime/demo-account.json"
$base = 'http://127.0.0.1:3000/api/v1'
function Assert-LocalDemoDatabase {
    $listener = Get-NetTCPConnection -State Listen -LocalPort 13306 -ErrorAction Stop
    if (@($listener).Count -ne 1 -or $listener.LocalAddress -ne '127.0.0.1') { throw 'Demo writes require the isolated loopback MySQL instance.' }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    $dataDir = Join-Path $root 'tools/native-runtime/mysql-data'
    if ($process.Name -ne 'mysqld.exe' -or -not $process.CommandLine.Replace('\','/').Contains("--datadir=$($dataDir.Replace('\','/'))")) { throw 'The database process does not use this project runtime directory. No demo writes allowed.' }
    foreach ($port in @(8101,8102,8105,8106)) {
        $service = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction Stop
        $connections = @(Get-NetTCPConnection -State Established -OwningProcess $service.OwningProcess -ErrorAction Stop | Where-Object RemotePort -eq 13306)
        if (-not $connections.Count -or @($connections | Where-Object RemoteAddress -ne '127.0.0.1').Count) { throw "Service $port is not connected to the isolated local database. No demo writes allowed." }
    }
    Write-Host 'Verified local MySQL 127.0.0.1:13306, project data directory and backend connections.'
}
if ($PopulateWorkspace) { Assert-LocalDemoDatabase }
function Invoke-LocalApi([string]$Path, [string]$Method = 'Get', $Body = $null, $Headers = @{}) {
    $parameters = @{ Uri = "$base$Path"; Method = $Method; Headers = $Headers }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $Body -Depth 8))
    }
    try { $result = Invoke-RestMethod @parameters }
    catch { throw "$Method $Path failed: $($_.Exception.Message)" }
    if ($result.code -ne 200) { throw "$Path failed ($($result.code)): $($result.message)" }
    return $result.data
}
if (Test-Path $accountFile) {
    $account = Get-Content $accountFile -Raw | ConvertFrom-Json
} else {
    $account = [pscustomobject]@{ username = 'demo'; password = 'Demo' + [Guid]::NewGuid().ToString('N').Substring(0, 12); phone = '13800000001' }
    Invoke-LocalApi '/auth/register' 'Post' @{ username = $account.username; password = $account.password } | Out-Null
    $account | ConvertTo-Json | Set-Content $accountFile -Encoding UTF8
    Write-Host 'Created local demo account (credentials retained in ignored runtime directory).'
}
$login = Invoke-LocalApi '/auth/login' 'Post' @{ account = $account.username; password = $account.password }
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$user = Invoke-LocalApi '/user/info' 'Get' $null $headers
Write-Host "Login verified: $($user.username)"
$spaces = @(Invoke-LocalApi '/space/my' 'Get' $null $headers)
if ($spaces.Count -eq 0) {
    Invoke-LocalApi '/space/create' 'Post' @{ spaceName = 'Demo Workspace' } $headers | Out-Null
    Write-Host 'Created demo workspace.'
}
Write-Host 'Local account and workspace ready.'

function Populate-DashboardWorkspace {
    if ($user.username -ne $account.username) { throw 'Dashboard fixtures are restricted to the configured local demo account.' }
    $teamName = '产品协作演示'
    $emptyTeamName = '空状态验收'
    $mySpaces = @(Invoke-LocalApi '/space/my' 'Get' $null $headers)
    $backup = @{spaces=$mySpaces;workspaces=@()}
    foreach($existingTeam in @($mySpaces | Where-Object { $_.spaceName -in @($teamName,$emptyTeamName) })) {
        $snapshotHeaders=@{Authorization=$headers.Authorization;'X-Space-Id'="$($existingTeam.spaceId)"}
        $backup.workspaces+=@{spaceId=$existingTeam.spaceId;documents=(Invoke-LocalApi "/document/list?spaceId=$($existingTeam.spaceId)&pageSize=100" 'Get' $null $snapshotHeaders);tasks=@(Invoke-LocalApi '/task/list' 'Get' $null $snapshotHeaders);files=@(Invoke-LocalApi "/file/list?spaceId=$($existingTeam.spaceId)" 'Get' $null $snapshotHeaders)}
    }
    $backup.meetings=@(Invoke-LocalApi '/meeting/list' 'Get' $null $headers)
    $backup|ConvertTo-Json -Depth 30|Set-Content "$root/tools/native-runtime/before-dashboard-workspace-$(Get-Date -Format 'yyyyMMdd-HHmmss-fff').json" -Encoding UTF8
    $team = $mySpaces | Where-Object spaceName -eq $teamName | Select-Object -First 1
    if (-not $team) { $team = Invoke-LocalApi '/space/create' 'Post' @{spaceName=$teamName} $headers }
    $emptyTeam = $mySpaces | Where-Object spaceName -eq $emptyTeamName | Select-Object -First 1
    if (-not $emptyTeam) { $emptyTeam = Invoke-LocalApi '/space/create' 'Post' @{spaceName=$emptyTeamName} $headers }
    $demoHeaders = @{Authorization=$headers.Authorization;'X-Space-Id'="$($team.spaceId)"}
    $emptyHeaders = @{Authorization=$headers.Authorization;'X-Space-Id'="$($emptyTeam.spaceId)"}
    $existing = @( (Invoke-LocalApi "/document/list?spaceId=$($team.spaceId)&status=1&pageSize=100" 'Get' $null $demoHeaders).records )
    $definitions = @(
        @{title='协同办公平台 · 项目概览';type=1;content='<h1>协同办公平台</h1><p>目标：为小型项目团队提供统一的文档、任务和沟通空间。</p><h2>本期范围</h2><ul><li>完成工作台与基础协作流程</li><li>统一界面和信息层级</li><li>验证数据保存及成员权限</li></ul>'},
        @{title='产品需求文档 · 工作台改版';type=1;content='<h1>工作台改版需求</h1><p>负责人：产品组 · 状态：评审中</p><h2>需求清单</h2><ul><li>一眼看清文档、待办和会议</li><li>任务入口清晰，空状态可操作</li><li>会议显示相对时间和状态</li></ul><h2>验收标准</h2><p>桌面和窄屏无重叠，统计和实际列表一致。</p>'},
        @{title='设计规范 · 界面与组件';type=1;content='<h1>界面设计规范</h1><p>主色采用协作蓝，绿色用于进行状态，琥珀色用于日程提示。</p><h2>布局</h2><p>采用 8px 间距体系；内容区保持克制留白，卡片圆角不超过 8px。</p><h2>可访问性</h2><p>图标操作有名称；键盘聚焦时保持可见；触屏保留操作按钮。</p>'},
        @{title='会议纪要 · 第一轮需求评审';type=1;content='<h1>第一轮需求评审</h1><p>参与：产品组、设计组、研发组</p><h2>讨论结论</h2><ul><li>优先完善已有主干流程</li><li>暂不接入外部短信与音视频</li><li>演示数据使用独立本地团队</li></ul><h2>下一步</h2><p>设计组完成工作台方案，研发组整理回归清单。</p>'},
        @{title='迭代周报 · 本周交付';type=1;content='<h1>迭代周报</h1><h2>本周完成</h2><p>打通文档保存、任务状态、会议纪要和文件下载流程。</p><h2>进行中</h2><p>工作台视觉优化与桌面、窄屏回归。</p><h2>下周计划</h2><p>准备答辩演示，梳理使用说明与已知边界。</p>'},
        @{title='验收清单 · 毕设演示';type=1;content='<h1>毕设演示验收清单</h1><ul><li>本地账号登录与团队切换</li><li>文档编辑、评论、历史版本</li><li>任务创建、分配与完成</li><li>预约会议和保存纪要</li><li>文件下载与表格导出</li><li>无待办时展示友好空状态</li></ul>'},
        @{title='项目排期 · 里程碑';type=2},
        @{title='资源预算 · 演示环境';type=2},
        @{title='产品规划 · 功能地图';type=3}
    )
    $documentIds = @{}
    foreach ($definition in $definitions) {
        $doc = $existing | Where-Object { $_.title -eq $definition.title -and $_.type -eq $definition.type } | Select-Object -First 1
        if ($doc) { $documentIds[$definition.title] = "$($doc.docId)"; continue }
        $docId = Invoke-LocalApi '/document/create' 'Post' @{title=$definition.title;type=$definition.type;spaceId=$team.spaceId} $demoHeaders
        $documentIds[$definition.title] = "$docId"
        if ($definition.type -eq 1) {
            $saved = Invoke-RestMethod "$base/document/$docId/content" -Method Put -Headers $demoHeaders -ContentType 'text/html; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($definition.content))
            if ($saved.code -ne 200) { throw "Demo document content save failed: $($definition.title)" }
        } elseif ($definition.type -eq 2) {
            $sheet = Invoke-LocalApi "/sheet/manage/$docId" 'Get' $null $demoHeaders
            $innerId = "$($sheet.sheets[0].id)"
            $rows = if ($definition.title -like '项目排期*') { @(@('里程碑','负责人','计划','状态'),@('需求评审','产品组','本周','已完成'),@('界面优化','设计组','下周','进行中'),@('答辩彩排','项目组','下下周','待开始')) } else { @(@('资源','数量','用途'),@('本地开发电脑','1','前后端与数据库'),@('测试账号','2','协作与权限验收'),@('外部付费 API','0','本机演示无需采购')) }
            $cells = @(for ($row=0; $row -lt $rows.Count; $row++) { for ($col=0; $col -lt $rows[$row].Count; $col++) { @{sheetId=$innerId;cellKey=([string][char](65+$col))+($row+1);content=$rows[$row][$col]} } })
            Invoke-LocalApi '/sheet/cell/batch' 'Put' @{sheetId=$innerId;cells=$cells} $demoHeaders | Out-Null
        } else {
            $map = Invoke-LocalApi "/mindmap/$docId" 'Get' $null $demoHeaders
            foreach ($topic in @('团队与账号','文档与知识','任务与日程','文件与沟通')) {
                Invoke-LocalApi "/mindmap/$docId/node?parentId=$($map.rootNode.id)&content=$([uri]::EscapeDataString($topic))" 'Post' $null $demoHeaders | Out-Null
            }
        }
    }
    $tasksBefore = @(Invoke-LocalApi '/task/list' 'Get' $null $demoHeaders)
    $taskDefinitions = @(
        @{title='整理工作台改版需求与验收标准';status=2;priority=2;days=-1},
        @{title='完成统计卡片与会议列表视觉走查';status=1;priority=2;days=0},
        @{title='补齐文档、任务与云盘回归截图';status=0;priority=3;days=1},
        @{title='准备毕业设计答辩演示与讲稿';status=0;priority=3;days=3}
    )
    foreach ($definition in $taskDefinitions) {
        if ($tasksBefore | Where-Object title -eq $definition.title) { continue }
        Invoke-LocalApi '/task/create' 'Post' @{title=$definition.title;description='产品协作演示团队的本地示例任务。';status=$definition.status;priority=$definition.priority;assigneeId=$user.userId;dueDate=(Get-Date).AddDays($definition.days).ToString('yyyy-MM-dd')} $demoHeaders | Out-Null
    }
    $meetingsBefore = @(Invoke-LocalApi '/meeting/list' 'Get' $null $demoHeaders)
    # Preserve historical local acceptance fixtures and their pre-change values.
    $oldFixtures = @($meetingsBefore | Where-Object { $_.title -eq 'Acceptance planning' -and $_.status -ne 4 -and "$($_.hostId)" -eq "$($user.userId)" -and [datetime]$_.endTime -gt (Get-Date) })
    if ($oldFixtures.Count) {
        Assert-LocalDemoDatabase
        $oldFixtures | ConvertTo-Json -Depth 5 | Set-Content "$root/tools/native-runtime/before-dashboard-meetings-$(Get-Date -Format 'yyyyMMdd-HHmmss').json" -Encoding UTF8
        $passwordLine = Get-Content "$root/.env" | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1
        if (-not $passwordLine) { throw 'Local database password configuration missing.' }
        $previousPassword = $env:MYSQL_PWD
        try {
            $env:MYSQL_PWD = $passwordLine.Substring('DB_PASSWORD='.Length).Trim().Trim('"').Trim("'")
            $index = 0
            foreach ($meeting in $oldFixtures) {
                if ("$($meeting.meetingId)" -notmatch '^\d+$' -or "$($user.userId)" -notmatch '^\d+$') { throw 'Invalid local fixture identifier.' }
                $index++
                $oldStart = (Get-Date).Date.AddDays(-$index).AddHours(10)
                $sql = "UPDATE meeting_room SET start_time='$($oldStart.ToString('yyyy-MM-dd HH:mm:ss'))', end_time='$($oldStart.AddHours(1).ToString('yyyy-MM-dd HH:mm:ss'))', update_time=NOW() WHERE room_id='$($meeting.meetingId)' AND host_id=$($user.userId) AND title='Acceptance planning';"
                & 'C:/DevTools/mysql-8.0.43-winx64/bin/mysql.exe' --host=127.0.0.1 --port=13306 --user=root --default-character-set=utf8mb4 collab_office "--execute=$sql"
                if ($LASTEXITCODE -ne 0) { throw 'Failed to archive local acceptance fixture dates.' }
            }
        } finally { $env:MYSQL_PWD = $previousPassword }
        Write-Host "Moved $($oldFixtures.Count) known acceptance meetings to historical dates; original records backed up."
    }
    $now = Get-Date
    $meetingDefinitions = @(
        @{title='工作台改版 · 设计走查';start=$now.AddMinutes(-10);end=$now.AddMinutes(50);agenda='检查统计卡片、空状态和列表交互。'},
        @{title='第二轮需求评审';start=$now.Date.AddDays(1).AddHours(10).AddMinutes(30);end=$now.Date.AddDays(1).AddHours(11).AddMinutes(15);agenda='确认主干流程和剩余验收事项。'},
        @{title='迭代计划与任务分工';start=$now.Date.AddDays(2).AddHours(14);end=$now.Date.AddDays(2).AddHours(15);agenda='安排下个迭代的优先级和责任人。'},
        @{title='毕业设计答辩彩排';start=$now.Date.AddDays(4).AddHours(15).AddMinutes(30);end=$now.Date.AddDays(4).AddHours(16).AddMinutes(30);agenda='走通演示流程，检查讲稿与材料。'}
    )
    foreach ($definition in $meetingDefinitions) {
        if ($meetingsBefore | Where-Object { $_.title -eq $definition.title -and $_.status -ne 4 -and [datetime]$_.endTime -gt $now }) { continue }
        Invoke-LocalApi '/meeting/create' 'Post' @{title=$definition.title;startTime=$definition.start.ToString('yyyy-MM-ddTHH:mm:ss');endTime=$definition.end.ToString('yyyy-MM-ddTHH:mm:ss');agenda=$definition.agenda;participantIds=@()} $demoHeaders | Out-Null
    }
    $files = @(Invoke-LocalApi "/file/list?spaceId=$($team.spaceId)" 'Get' $null $demoHeaders)
    foreach ($fileName in @('LOCAL-RUN.md','项目完善报告.md')) {
        if ($files | Where-Object fileName -eq $fileName) { continue }
        $uploaded = Invoke-RestMethod "$base/file/upload" -Method Post -Headers $demoHeaders -Form @{file=Get-Item (Join-Path $root $fileName)}
        if ($uploaded.code -ne 200) { throw 'Demo file upload failed.' }
    }
    $allDocuments = Invoke-LocalApi "/document/list?spaceId=$($team.spaceId)&status=1&pageSize=100" 'Get' $null $demoHeaders
    $allTasks = @(Invoke-LocalApi '/task/list' 'Get' $null $demoHeaders)
    $allMeetings = @(Invoke-LocalApi '/meeting/list' 'Get' $null $demoHeaders | Where-Object { $_.status -in @(1,2) -and [datetime]$_.endTime -gt (Get-Date) })
    $emptyTasks = @(Invoke-LocalApi '/task/list' 'Get' $null $emptyHeaders)
    if ($allDocuments.total -lt 9 -or $allTasks.Count -lt 4 -or $allMeetings.Count -lt 4 -or $emptyTasks.Count -ne 0) { throw 'Dashboard fixture read-back failed.' }
    $summary = @{spaceId="$($team.spaceId)";spaceName=$teamName;emptySpaceId="$($emptyTeam.spaceId)";emptySpaceName=$emptyTeamName;documents=[int]$allDocuments.total;pendingTasks=@($allTasks | Where-Object status -lt 2).Count;meetings=$allMeetings.Count;documentIds=$documentIds}
    $summary | ConvertTo-Json -Depth 5 | Set-Content "$root/tools/native-runtime/dashboard-demo.json" -Encoding UTF8
    Write-Host "Dashboard demo ready: $($summary.documents) documents, $($summary.pendingTasks) pending tasks, $($summary.meetings) meetings. Empty-state team verified."
}
if ($PopulateWorkspace) { Populate-DashboardWorkspace }
