package pj.domain

type ResultSchedule[A] = Either[ScheduleError, A]

final case class ScheduleError(error: String)
