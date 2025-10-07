{{- define "owl-app.fullname" -}}
{{- printf "%s-%s" .Release.Name "owlapp" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
