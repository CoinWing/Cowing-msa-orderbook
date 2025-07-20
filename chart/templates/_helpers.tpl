{{/*
Generate a name for the application
*/}}
{{- define "msa-orderbook.name" -}}
msa-orderbook
{{- end }}

{{/*
Generate the full name including release name
*/}}
{{- define "msa-orderbook.fullname" -}}
{{ .Release.Name }}-msa-orderbook
{{- end }}
