<?php

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

$messages = [];

if(!isset($_GET['package_name']))
{
	return json_response(NULL, 1, ["Param 'package_name' is missing"]);
}
if(!isset($_GET['version_code']))
{
	return json_response(NULL, 1, ["Param 'version_code' is missing"]);
}

if(!isset($_GET['type']))
{
	array_push($messages, "'type' not set, defaulting to release");
	$type = 'release';
}
else
{
	$type = $_GET['type'];
}
$version_code = $_GET['version_code'];
$package_name = $_GET['package_name'];

if(!is_numeric($version_code))
{
	return json_response(NULL, 1, ["Param 'version_code' must be a number"]);
}

$package_name_dir = getcwd()."/".$package_name."/".$type; 

if(!file_exists($package_name_dir))
{
	return json_response(NULL, 1, ["Application does not exist"]);
}

$versions = [];

if ($handle = opendir($package_name_dir)) 
{
	$blacklist = ['.', '..'];
    while (false !== ($file = readdir($handle))) 
    {
        if (!in_array($file, $blacklist) 
        	&& is_numeric($file)
        	&& is_dir($package_name_dir."/".$file)) 
        {
            array_push($versions, $file);
        }
    }
    closedir($handle);
}
sort($versions);

if(empty($versions))
{
	return json_response(NULL, 1, ["Application does not have any version"]);
}

$last = array_slice($versions, -1)[0];

if($last == $version_code || (isset($_GET['force_full_download']) && $_GET['force_full_download'] == TRUE))
{
	$patch_file_name = "classes.dex";
	$patch_file = $package_name_dir."/".$last."/".$patch_file_name;
	$md5_file = $patch_file.".md5";
}
else
{
	$patch_file_name = $version_code."_".$last.".patch";
	$patch_file = $package_name_dir."/".$last."/".$patch_file_name;
	$md5_file = $patch_file.".md5";
}

if(!file_exists($patch_file))
{
	return json_response(NULL, 1, ["There is no patch for your version"]);
}

if(!file_exists($md5_file))
{
	return json_response(NULL, 1, ["md5 file not found on server"]);
}

$url_root = "http://".$_SERVER['HTTP_HOST'].dirname($_SERVER['PHP_SELF']).$package_name."/".$type."/".$last."/";

$data = [];
$data['classes'] = $url_root.$patch_file_name;
$data['md5'] =  $url_root.$patch_file_name.".md5";
$data['version'] = $last;


return json_response($data);

function json_response($data, $error = 0, array $messages = NULL)
{
	$response = [];
	$response['error'] = $error;
	$response['messages'] = $messages;
	$response['data'] = $data;
	echo json_encode($response);
}