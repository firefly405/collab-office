import {createRouter,createWebHashHistory} from 'vue-router'
import {hasToken} from '@/utils/token'
import {useIdentityStore} from '@/stores/identity'
const router=createRouter({history:createWebHashHistory(),routes:[
  {path:'/login',component:()=>import('@/views/auth/LoginPage.vue'),meta:{requiresAuth:false}},
  {path:'/register',component:()=>import('@/views/auth/RegisterPage.vue'),meta:{requiresAuth:false}},
  {path:'/share/:token',component:()=>import('@/views/doc/DocEditorPage.vue'),meta:{requiresAuth:false}},
  {path:'/',component:()=>import('@/layouts/MainLayout.vue'),redirect:'/dashboard',children:[
    {path:'dashboard',component:()=>import('@/views/roles/DashboardPage.vue')},
    {path:'reports',component:()=>import('@/views/roles/ReportPage.vue')},
    {path:'admin',component:()=>import('@/views/roles/SystemAdminPage.vue'),meta:{systemAdmin:true}},
    {path:'documents',component:()=>import('@/views/doc/DocListPage.vue')},
    {path:'templates',component:()=>import('@/views/doc/TemplateCenter.vue')},
    {path:'knowledge',component:()=>import('@/views/file/KnowledgeListPage.vue')},
    {path:'recycle',component:()=>import('@/views/doc/DocListPage.vue')},
    {path:'documents/:docId',component:()=>import('@/views/doc/DocEditorPage.vue')},
    {path:'sheets',component:()=>import('@/views/doc/DocListPage.vue')},
    {path:'sheets/:docId',component:()=>import('@/views/sheet/SpreadsheetEditor.vue')},
    {path:'mindmap',component:()=>import('@/views/doc/DocListPage.vue')},
    {path:'mindmap/:docId',component:()=>import('@/views/mindmap/MindmapEditorPage.vue')},
    {path:'whiteboard',component:()=>import('@/views/doc/DocListPage.vue')},
    {path:'whiteboard/:docId',component:()=>import('@/views/whiteboard/WhiteboardEditorPage.vue')},
    {path:'im',component:()=>import('@/views/im/IMLayout.vue')},
    {path:'tasks',component:()=>import('@/views/task/TaskKanbanPage.vue')},
    {path:'tasks/list',component:()=>import('@/views/task/TaskKanbanPage.vue')},
    {path:'tasks/calendar',component:()=>import('@/views/task/TaskKanbanPage.vue')},
    {path:'files',component:()=>import('@/views/file/FileManagePage.vue')},
    {path:'files/preview/:fileId',component:()=>import('@/views/file/FilePreviewPage.vue')},
    {path:'meetings',component:()=>import('@/views/meeting/MeetingListPage.vue')},
    {path:'meetings/schedule',component:()=>import('@/views/meeting/MeetingListPage.vue')},
    {path:'meetings/history',component:()=>import('@/views/meeting/MeetingListPage.vue')},
    {path:'settings',component:()=>import('@/views/settings/PersonalSettingsPage.vue')},
    {path:'statistics',component:()=>import('@/views/settings/WorkspaceStatisticsPage.vue')},
    {path:'settings/notifications',component:()=>import('@/views/settings/NotificationCenter.vue')},
    {path:'space/manage',component:()=>import('@/views/org/SpaceManagePage.vue')},
    {path:'search',component:()=>import('@/views/search/SearchResultPage.vue')},
  ]},
  {path:'/:pathMatch(.*)*',redirect:'/dashboard'},
]})
router.beforeEach(async to=>{
  if(to.meta.requiresAuth===false)return true
  if(!hasToken())return '/login'
  const identity=useIdentityStore()
  try{await identity.load()}catch{return '/login'}
  return to.meta.systemAdmin&&!identity.isAdmin?'/dashboard':true
})
export default router
