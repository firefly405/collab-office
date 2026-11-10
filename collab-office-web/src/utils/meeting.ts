export const isCancelledMeeting = (meeting: { status: number }) => meeting.status === 4
export const isUpcomingMeeting = (meeting: { status: number; startTime: string; endTime?: string }) =>
  !isCancelledMeeting(meeting) && meeting.status !== 3 && new Date(meeting.endTime || meeting.startTime).getTime() > Date.now()
