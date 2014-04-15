<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<html>
<head>
	<title>图像搜索管理</title>
</head>

<body>
	<form id="inputForm" action="s" method="post" class="form-horizontal" enctype="multipart/form-data">

		<fieldset>
			<legend><small>图像搜索管理</small></legend>
			<div class="control-group">
				<input id="reindex-btn" class="btn" type="button" value="重新建立图像索引"/>
				<input id="index-btn" class="btn" type="button" value="增量建立图像索引"/>
			</div>
			<div class="control-group">
				图像文件：<input type="file" class="span3" name="file" value="${file}"/><br/>
				关键字：<input type="text" class="span3" name="title" value="${title}" />
			</div>
			<div class="control-group">
				经度：<input type="text" class="span2" name="lng" value="${lng}" />
				纬度：<input type="text" class="span2" name="lat" value="${lat}" />
			</div>
			<div class="control-group">
				<!-- 
				检索方法：
				<select name="method">
					<option value="0" selected="true">0</option>
					<option value="1">1</option>
					<option value="2">2</option>
					<option value="3">3</option>
					<option value="4">4</option>
					<option value="5">5</option>
					<option value="6">6</option>
					<option value="7">7</option>
					<option value="8">8</option>
					<option value="9">9</option>
					<option value="10">10</option>
					<option value="11">11</option>
					<option value="12">12</option>
					<option value="13">13</option>
					<option value="14">14</option>
					<option value="15">15</option>
					<option value="16">16</option>
					<option value="17">17</option>
					<option value="18">18</option>
					<option value="19">19</option>
				</select>
				 -->
				<input type="hidden" name="method" value="0" />
				<input id="search-btn" class="btn btn-primary" type="submit" value="搜索图像"/>
			</div>
			<c:if test="${fn:length(result) > 0 }">
			<table id="contentTable" class="table table-striped table-bordered table-condensed">
				<thead><tr><th>图像</th><th>相似距离</th><th>标题</th><th>标签<th>位置</th></tr></thead>
				<tbody>
				<c:forEach items="${result}" var="r">
					<tr>
						<td>
							<a href="image?id=${r.id}" target="_blank" class="image-result">
								<img alt="" src="image?id=${r.id}" >
							</a>
						</td>
						<td>${r.distance}</td>
						<td>${r.title}</td>
						<td>${r.tags}</td>
						<td>${r.location}（${r.lng}, ${r.lat}）</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
			</c:if>
		</fieldset>
	</form>
	<script>
		$(document).ready(function() {
			$('#index-btn').click(function () {
				$('#inputForm').attr('action', 'index');
				$('#inputForm').submit();
			});
			$('#reindex-btn').click(function () {
				$('#inputForm').attr('action', 'reindex');
				$('#inputForm').submit();
			});
		});
	</script>
</body>
</html>
