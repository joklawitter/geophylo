from celery import Celery

celery = Celery(__name__)
celery.conf.broker_url = "redis://redis"
celery.conf.result_backend = "redis://redis"
celery.conf.task_ignore_result = True
